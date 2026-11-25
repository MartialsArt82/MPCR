#!/usr/bin/env python3
"""Single-client Remote Screen v2 fixture, reachable only via local loopback."""

import argparse
import hashlib
import hmac
import socket
import struct


def packet(kind, payload=b""):
    return struct.pack("<BI", kind, len(payload)) + payload


def read_message(stream):
    header = stream.read(5)
    if not header:
        return None, b""
    kind, count = struct.unpack("<BI", header)
    return kind, stream.read(count)


def rectangle(x, y, w, h, codec, pixel_bytes):
    return struct.pack("<HHHHBBI", x, y, w, h, codec, 0, len(pixel_bytes)) + pixel_bytes


def serve(port, mode):
    with socket.socket() as server:
        server.bind(("127.0.0.1", port))
        server.listen(1)
        print(server.getsockname()[1], flush=True)
        conn, _ = server.accept()
        with conn, conn.makefile("rb") as stream:
            conn.settimeout(45)
            nonce = bytes(range(16))
            conn.sendall(packet(1, b"RSCR" + struct.pack("<HH", 2, 1) + nonce))
            kind, auth = read_message(stream)
            assert kind == 2 and len(auth) == 50
            assert auth[48:] == b"\x1b\x01"
            password = "" if mode == "empty" else "MPC Ä"
            key = hashlib.pbkdf2_hmac("sha256", password.encode(), b"remote-screen/v1", 10000)
            nonces = nonce + auth[:16]
            expected = hmac.digest(key, b"RSCR-client" + nonces, "sha256")
            if not hmac.compare_digest(auth[16:48], expected):
                conn.sendall(packet(3, b"\x00\x03bad"))
                assert mode == "reject"
                return
            proof = hmac.digest(key, b"RSCR-server" + nonces, "sha256")
            if mode == "bad-proof":
                conn.sendall(packet(3, b"\x01" + bytes(32)))
                return
            config = struct.pack("<HH", 400, 240) + bytes((32, 16, 8, 8, 8, 0, 8))
            config += b"\x03MPC" + bytes((1, 1, 1))
            frame = struct.pack("<IBH", 42, 0, 3)
            frame += rectangle(0, 0, 400, 240, 3, b"\xd0\x80\x20\x00")
            frame += rectangle(0, 0, 2, 1, 0, b"\x00\x00\xff\x00\x00\xff\x00\x00")
            frame += rectangle(399, 239, 1, 1, 1, b"\x40\xff\x00\xff\x00")
            conn.sendall(packet(3, b"\x01" + proof) + packet(4, config) + packet(5, frame))
            seen = set()
            while True:
                kind, payload = read_message(stream)
                if kind is None:
                    break
                if kind == 8:
                    assert payload == b"\x00\x1b"
                    seen.add("options")
                elif kind == 6:
                    assert payload == struct.pack("<I", 42)
                    seen.add("ack")
                elif kind == 9:
                    conn.sendall(packet(10, payload))
                elif kind == 12:
                    count = payload[0]
                    for i in range(count):
                        event, _, x, y, _ = struct.unpack_from("<BBHHh", payload, 1 + i * 8)
                        assert 0 <= x < 240 and 0 <= y < 400
                        seen.add(f"input-{event}")
                        print(f"input-{event} {x} {y}", flush=True)
                elif kind == 13 and payload[0]:
                    assert payload == b"\x01\x01"
                    conn.sendall(packet(14, struct.pack("<IBB", 48000, 2, 0)))
                    conn.sendall(packet(15, struct.pack("<QHhh", 0, 1, 1234, -1234)))
                    seen.add("audio")
                required = {"options", "ack", "input-0", "input-1", "input-2", "audio"}
                if required <= seen:
                    print("ACK, drag and PCM request verified", flush=True)
                    conn.sendall(packet(11))
                    return
            if mode != "visual":
                raise AssertionError(f"Connection closed before checks finished ({seen})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--mode", choices=("normal", "empty", "reject", "bad-proof", "visual"),
                        default="normal")
    args = parser.parse_args()
    serve(args.port, args.mode)
