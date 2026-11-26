package mpc.remote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

final class ScreenBuffer {
    interface Jpeg {
        int[] decode(byte[] bytes, int offset, int length, int width, int height) throws IOException;
    }

    interface PixelRegion {
        void copy(int[] pixels, int offset, int stride, int x, int y, int width, int height);
    }

    private static final int MAX_PIXELS = 4 * 1024 * 1024;
    private final int nativeWidth;
    private final int nativeHeight;
    private final int rotation;
    private final int bytesPerPixel;
    private final int[] shifts = new int[3];
    private final int[] bits = new int[3];
    private final int[] pixels;
    private final Jpeg jpeg;
    private int left;
    private int top;
    private int right;
    private int bottom;
    final int width;
    final int height;
    final int controls;
    final boolean audio;

    ScreenBuffer(byte[] description, Jpeg jpeg) throws IOException {
        this.jpeg = jpeg;
        ByteBuffer data = RemoteSession.little(ByteBuffer.wrap(description));
        if (data.remaining() < 14) throw malformed();
        nativeWidth = u16(data);
        nativeHeight = u16(data);
        int depth = u8(data);
        if (nativeWidth == 0 || nativeHeight == 0 || (long) nativeWidth * nativeHeight > MAX_PIXELS
                || (depth != 16 && depth != 24 && depth != 32)) throw malformed();
        bytesPerPixel = depth / 8;
        for (int i = 0; i < 3; i++) {
            shifts[i] = u8(data);
            bits[i] = u8(data);
            if (bits[i] < 1 || bits[i] > 8 || shifts[i] + bits[i] > depth) throw malformed();
        }
        int nameSize = u8(data);
        if (data.remaining() < nameSize + 2) throw malformed();
        data.position(data.position() + nameSize);
        rotation = u8(data);
        if (rotation > 3) throw malformed();
        controls = u8(data);
        audio = data.hasRemaining() && u8(data) != 0;
        width = (rotation & 1) == 0 ? nativeWidth : nativeHeight;
        height = (rotation & 1) == 0 ? nativeHeight : nativeWidth;
        pixels = new int[width * height];
        Arrays.fill(pixels, 0xff000000);
        clearDamage();
    }

    synchronized int update(byte[] payload) throws IOException {
        ByteBuffer bytes = RemoteSession.little(ByteBuffer.wrap(payload));
        if (bytes.remaining() < 7) throw malformed();
        int id = bytes.getInt();
        bytes.get(); // Quality level.
        int rectangles = u16(bytes);
        int budget = 4 * MAX_PIXELS;
        for (int i = 0; i < rectangles; i++) {
            if (bytes.remaining() < 14) throw malformed();
            int x = u16(bytes);
            int y = u16(bytes);
            int w = u16(bytes);
            int h = u16(bytes);
            int codec = u8(bytes);
            int drop = u8(bytes);
            int length = bytes.getInt();
            int offset = bytes.position();
            if (length < 0 || length > bytes.remaining()) throw malformed();
            bytes.position(offset + length);
            if (w == 0 || h == 0 || x + w > nativeWidth || y + h > nativeHeight || w * h > budget) continue;
            budget -= w * h;
            apply(payload, offset, length, x, y, w, h, codec, drop);
        }
        return id;
    }

    private void apply(byte[] data, int offset, int length, int x, int y,
                       int w, int h, int codec, int drop) {
        int[] rgb = null;
        int expected = w * h * bytesPerPixel;
        switch (codec) {
            case 0:
                if (length != expected) return;
                break;
            case 1:
                data = lz4(data, offset, length, expected);
                if (data == null) return;
                offset = 0;
                break;
            case 3:
                if (length != bytesPerPixel) return;
                break;
            case 4:
                if (bytesPerPixel != 4 || bits[0] != 8 || bits[1] != 8 || bits[2] != 8
                        || shifts[1] != 8 || shifts[0] + shifts[2] != 16
                        || (shifts[0] != 0 && shifts[0] != 16)) return;
                try {
                    rgb = jpeg.decode(data, offset, length, w, h);
                    if (rgb == null || rgb.length != w * h) return;
                } catch (IOException | RuntimeException e) {
                    return;
                }
                break;
            default:
                return;
        }
        int solid = codec == 3 ? unpack(data, offset, drop) : 0;
        for (int row = 0; row < h; row++) {
            for (int column = 0; column < w; column++) {
                int i = row * w + column;
                int color;
                if (codec == 3) color = solid;
                else if (rgb != null) color = jpegPixel(rgb[i], drop);
                else color = unpack(data, offset + i * bytesPerPixel, drop);
                pixels[rotatedIndex(x + column, y + row)] = color;
            }
        }
        int first = rotatedIndex(x, y);
        int last = rotatedIndex(x + w - 1, y + h - 1);
        left = Math.min(left, Math.min(first % width, last % width));
        right = Math.max(right, Math.max(first % width, last % width) + 1);
        top = Math.min(top, Math.min(first / width, last / width));
        bottom = Math.max(bottom, Math.max(first / width, last / width) + 1);
    }

    void copyTo(int[] destination, boolean full) {
        copyTo((source, offset, stride, x, y, w, h) -> {
            if (w == stride) {
                System.arraycopy(source, offset, destination, offset, w * h);
            } else {
                for (int row = 0; row < h; row++) {
                    int start = offset + row * stride;
                    System.arraycopy(source, start, destination, start, w);
                }
            }
        }, full);
    }

    // The display owns one destination and consumes all changes since its previous paint.
    synchronized void copyTo(PixelRegion destination, boolean full) {
        if (full) {
            destination.copy(pixels, 0, width, 0, 0, width, height);
        } else if (left < right && top < bottom) {
            destination.copy(pixels, top * width + left, width, left, top, right - left, bottom - top);
        }
        clearDamage();
    }

    private void clearDamage() {
        left = width;
        top = height;
        right = 0;
        bottom = 0;
    }

    private static int jpegPixel(int rgb, int drop) {
        return 0xff000000 | (expand((rgb >>> 16) & 255, 8, drop) << 16)
                | (expand((rgb >>> 8) & 255, 8, drop) << 8) | expand(rgb & 255, 8, drop);
    }

    private int unpack(byte[] data, int offset, int drop) {
        int word = 0;
        for (int i = 0; i < bytesPerPixel; i++) word |= (data[offset + i] & 255) << (8 * i);
        int color = 0xff000000;
        for (int i = 0; i < 3; i++) {
            int channel = (word >>> shifts[i]) & ((1 << bits[i]) - 1);
            color |= expand(channel, bits[i], drop) << (16 - 8 * i);
        }
        return color;
    }

    private static int expand(int channel, int bits, int drop) {
        drop = Math.min(drop, bits - 1);
        if (drop > 0) channel |= channel >>> (bits - drop);
        int expanded = channel;
        for (int size = bits; size < 8; size += bits) expanded = (expanded << bits) | channel;
        int excess = (bits - (8 % bits)) % bits;
        return expanded >>> excess;
    }

    private int rotatedIndex(int x, int y) {
        switch (rotation) {
            case 1: return x * width + nativeHeight - 1 - y;
            case 2: return (nativeHeight - 1 - y) * width + nativeWidth - 1 - x;
            case 3: return (nativeWidth - 1 - x) * width + y;
            default: return y * width + x;
        }
    }

    static byte[] lz4(byte[] source, int start, int length, int expected) {
        byte[] decoded = new byte[expected];
        int end = start + length;
        int in = start;
        int out = 0;
        while (in < end) {
            int token = source[in++] & 255;
            int literals = token >>> 4;
            if (literals == 15) {
                int extra;
                do {
                    if (in >= end || literals > expected) return null;
                    extra = source[in++] & 255;
                    literals += extra;
                } while (extra == 255);
            }
            if (literals > end - in || literals > expected - out) return null;
            System.arraycopy(source, in, decoded, out, literals);
            in += literals;
            out += literals;
            if (in == end) return out == expected ? decoded : null;
            if (end - in < 2) return null;
            int distance = (source[in++] & 255) | ((source[in++] & 255) << 8);
            if (distance == 0 || distance > out) return null;
            int match = (token & 15) + 4;
            if ((token & 15) == 15) {
                int extra;
                do {
                    if (in >= end || match > expected) return null;
                    extra = source[in++] & 255;
                    match += extra;
                } while (extra == 255);
            }
            if (match > expected - out) return null;
            for (int i = 0; i < match; i++, out++) decoded[out] = decoded[out - distance];
        }
        return out == expected ? decoded : null;
    }

    private static int u8(ByteBuffer bytes) { return bytes.get() & 255; }
    private static int u16(ByteBuffer bytes) { return bytes.getShort() & 65535; }
    private static IOException malformed() { return new IOException("Invalid screen data from the MPC."); }
}
