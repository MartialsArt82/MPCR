package mpc.remote;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

final class PcmPlayer implements RemoteSession.Audio {
    interface Output extends AutoCloseable {
        void write(byte[] pcm);
        void close();
    }

    interface Factory {
        Output open(int rate, int channels) throws Exception;
    }

    private static final class Format {
        final int rate;
        final int channels;

        Format(int rate, int channels) {
            this.rate = rate;
            this.channels = channels;
        }
    }

    private static final class Packet {
        final Format format;
        final byte[] samples;

        Packet(Format format, byte[] samples) {
            this.format = format;
            this.samples = samples;
        }
    }

    private final ArrayBlockingQueue<Packet> queue = new ArrayBlockingQueue<>(6);
    private final Thread worker;
    private volatile Format format = new Format(0, 0);

    PcmPlayer(Factory factory) {
        worker = new Thread(() -> play(factory), "mpc-audio");
        worker.setDaemon(true);
        worker.start();
    }

    @Override public void format(int rate, int channels) {
        format = new Format(rate, channels);
        silence();
    }

    @Override public void pcm(byte[] data, int offset, int length) {
        Packet packet = new Packet(format, Arrays.copyOfRange(data, offset, offset + length));
        if (!queue.offer(packet)) {
            queue.poll();
            queue.offer(packet);
        }
    }

    @Override public void silence() {
        queue.clear();
    }

    @Override public void close() {
        worker.interrupt();
    }

    private void play(Factory factory) {
        Output output = null;
        Format last = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Packet packet = queue.take();
                Format current = packet.format;
                if (current != format) continue;
                if (last != current) {
                    if (output != null) output.close();
                    output = null;
                    last = current;
                    if (current.rate > 0) {
                        try {
                            output = factory.open(current.rate, current.channels);
                        } catch (Exception ignored) {
                            // Leave screen streaming when the system has no compatible audio output.
                        }
                    }
                }
                if (output != null && current == format) output.write(packet.samples);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (output != null) output.close();
        }
    }
}
