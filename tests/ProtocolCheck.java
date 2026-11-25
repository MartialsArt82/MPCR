package mpc.remote;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ProtocolCheck {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String mode = args[1];
        boolean valid = "normal".equals(mode) || "empty".equals(mode);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<byte[]> pcm = new AtomicReference<>();
        RemoteSession.Audio audio = new RemoteSession.Audio() {
            @Override public void format(int rate, int channels) {
                if (rate != 0 && (rate != 48000 || channels != 2)) throw new AssertionError("audio format");
            }
            @Override public void pcm(byte[] bytes, int off, int len) {
                pcm.set(Arrays.copyOfRange(bytes, off, off + len));
            }
            @Override public void silence() {}
            @Override public void close() {}
        };
        RemoteSession[] holder = new RemoteSession[1];
        RemoteSession.Listener listener = new RemoteSession.Listener() {
            @Override public void status(String text) {}
            @Override public void screen(ScreenBuffer screen) {
                if (screen.width != 240 || screen.height != 400 || screen.controls != 1 || !screen.audio) {
                    throw new AssertionError("screen description");
                }
            }
            @Override public void frame() {
                RemoteSession session = holder[0];
                ScreenBuffer frame = session.latestScreen();
                int[] pixels = new int[frame.width * frame.height];
                frame.copyTo(pixels, true);
                if (pixels[239] != 0xffff0000 || pixels[479] != 0xff00ff00
                        || pixels[399 * 240] != 0xffff00ff || pixels[0] != 0xff2080d0) {
                    throw new AssertionError("rotated framebuffer");
                }
                session.setAudio(true);
                session.pointer(0, 10, 20);
                session.pointer(1, 15, 26);
                session.pointer(2, 15, 26);
            }
            @Override public void ended(String text) {
                result.set(text);
                completed.countDown();
            }
        };
        try (RemoteSession session = new RemoteSession("127.0.0.1", port, listener, null, audio)) {
            holder[0] = session;
            session.start(mode.equals("empty") ? "" : mode.equals("reject") ? "incorrect" : "MPC Ä");
            if (!completed.await(12, TimeUnit.SECONDS)) throw new AssertionError("timeout");
        }
        String expected = valid ? "MPC hat die Verbindung beendet"
                : mode.equals("reject") ? "Anmeldung abgelehnt" : "Passwort nicht bestätigen";
        if (!result.get().contains(expected)) throw new AssertionError(result.get());
        if (valid && !Arrays.equals(pcm.get(), new byte[] {(byte) 0xd2, 4, 46, (byte) 0xfb})) {
            throw new AssertionError("PCM data");
        }
        byte[] block = {0x40, 1, 2, 3, 4, 4, 0, 0x50, 5, 6, 7, 8, 9};
        byte[] expanded = {1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        if (!Arrays.equals(ScreenBuffer.lz4(block, 0, block.length, expanded.length), expanded)) {
            throw new AssertionError("LZ4 back reference");
        }
        if (ScreenBuffer.lz4(block, 0, block.length, 4) != null) {
            throw new AssertionError("LZ4 length bound");
        }
        System.out.println(mode + " OK");
    }
}
