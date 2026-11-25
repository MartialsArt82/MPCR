package mpc.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class RemoteSession implements AutoCloseable {
    // Raw, LZ4 block, solid and JPEG. Codec negotiation excludes Zstandard.
    static final int SCREEN_CODECS = 0x1b;
    static final int DEFAULT_PORT = 7447;
    private static final int MAX_FRAME_MESSAGE = 32 * 1024 * 1024;

    interface Listener {
        void status(String text);
        void screen(ScreenBuffer screen);
        void frame();
        void ended(String reason);
    }

    interface Audio extends AutoCloseable {
        void format(int rate, int channels);
        void pcm(byte[] data, int offset, int length);
        void silence();
        void close();
    }

    private final Socket socket = new Socket();
    private final ArrayBlockingQueue<byte[]> outgoing = new ArrayBlockingQueue<>(96);
    private final Listener listener;
    private final ScreenBuffer.Jpeg jpeg;
    private final Audio audio;
    private final String host;
    private final int port;
    private volatile boolean closed;
    private volatile boolean audioEnabled;
    private volatile ScreenBuffer screen;
    private volatile ScreenBuffer latestScreen;
    private volatile String writerError;
    private OutputStream output;
    private int audioChannels;

    RemoteSession(String host, int port, Listener listener, ScreenBuffer.Jpeg jpeg, Audio audio) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        this.jpeg = jpeg;
        this.audio = audio;
    }

    void start(String password) {
        Thread thread = new Thread(() -> run(password), "mpc-receive");
        thread.setDaemon(true);
        thread.start();
    }

    @Override public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    ScreenBuffer latestScreen() {
        return latestScreen;
    }

    void setAudio(boolean enabled) {
        audioEnabled = enabled;
        if (!enabled) audio.silence();
        ScreenBuffer current = screen;
        if (current != null && current.audio) {
            enqueue(13, audioRequest(enabled));
        }
    }

    void pointer(int phase, int x, int y) {
        ScreenBuffer current = screen;
        if (current == null) return;
        if ((current.controls & 1) != 0) {
            enqueue(12, events(phase, x, y));
        } else if ((current.controls & 2) != 0) {
            enqueue(12, phase == 0 ? events(3, x, y, 4, x, y)
                    : events(phase == 1 ? 3 : 5, x, y));
        }
    }

    void releaseInput() {
        if (screen != null && (screen.controls & 3) != 0) enqueue(12, events(7, 0, 0));
    }

    private static byte[] events(int... entries) {
        ByteBuffer bytes = little(ByteBuffer.allocate(1 + 8 * (entries.length / 3)));
        bytes.put((byte) (entries.length / 3));
        for (int i = 0; i < entries.length; i += 3) {
            bytes.put((byte) entries[i]).put((byte) 0);
            bytes.putShort((short) entries[i + 1]).putShort((short) entries[i + 2]);
            bytes.putShort((short) 0);
        }
        return bytes.array();
    }

    private static byte[] audioRequest(boolean enable) {
        return new byte[] {(byte) (enable ? 1 : 0), 1}; // PCM16
    }

    private void enqueue(int type, byte[] payload) {
        if (!closed && !outgoing.offer(message(type, payload))) {
            writerError = "Die Verbindung verarbeitet Nachrichten zu langsam.";
            close();
        }
    }

    private void run(String password) {
        String reason = "Getrennt";
        try {
            listener.status("Verbindung wird aufgebaut …");
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(8000);
            socket.setTcpNoDelay(true);
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 65536));
            output = new BufferedOutputStream(socket.getOutputStream());
            authenticate(input, password);
            socket.setSoTimeout(20000);
            configure(expect(input, 4));
            Thread writer = new Thread(this::writeLoop, "mpc-send");
            writer.setDaemon(true);
            writer.start();
            try {
                while (!closed) {
                    Message item = receive(input);
                    if (!process(item)) {
                        reason = "Die MPC hat die Verbindung beendet.";
                        break;
                    }
                }
            } finally {
                writer.interrupt();
            }
        } catch (SocketTimeoutException e) {
            reason = "Zeitüberschreitung. Prüfe MPC-Adresse und Remote Screen.";
        } catch (EOFException e) {
            reason = "Die Verbindung zur MPC wurde geschlossen.";
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            reason = "Verbindung fehlgeschlagen. " + e.getMessage();
        } finally {
            boolean cancelled = closed;
            close();
            audio.close();
            listener.ended(writerError != null ? writerError : cancelled ? "Getrennt" : reason);
        }
    }

    private boolean process(Message item) throws IOException {
        switch (item.type) {
            case 4:
                configure(item.data);
                break;
            case 5:
                ScreenBuffer current = screen;
                int id = current.update(item.data);
                enqueue(6, little(ByteBuffer.allocate(4)).putInt(id).array());
                latestScreen = current;
                listener.frame();
                break;
            case 9:
                if (item.data.length == 8) enqueue(10, item.data);
                break;
            case 11:
                return false;
            case 14:
                audioFormat(item.data);
                break;
            case 15:
                audioPacket(item.data);
                break;
            default:
                break;
        }
        return true;
    }

    private void configure(byte[] payload) throws IOException {
        ScreenBuffer next = new ScreenBuffer(payload, jpeg);
        latestScreen = null;
        screen = next;
        audioChannels = 0;
        audio.format(0, 0);
        enqueue(8, new byte[] {0, SCREEN_CODECS});
        if (next.audio && audioEnabled) enqueue(13, audioRequest(true));
        listener.screen(next);
        listener.status("Verbunden mit " + host);
    }

    private void audioFormat(byte[] data) {
        if (data.length != 6) return;
        ByteBuffer bytes = little(ByteBuffer.wrap(data));
        int rate = bytes.getInt();
        int channels = bytes.get() & 0xff;
        int codec = bytes.get() & 0xff;
        if (codec != 0 || channels < 1 || channels > 2 || rate < 8000 || rate > 192000) {
            audioChannels = 0;
            audio.format(0, 0);
            return;
        }
        audioChannels = channels;
        audio.format(rate, channels);
    }

    private void audioPacket(byte[] data) {
        if (!audioEnabled || audioChannels == 0 || data.length < 10) return;
        int frames = (data[8] & 255) | ((data[9] & 255) << 8);
        int length = frames * audioChannels * 2;
        if (frames > 0 && frames <= 8192 && data.length == 10 + length) {
            audio.pcm(data, 10, length);
        }
    }

    private void writeLoop() {
        long pingAt = System.nanoTime();
        try {
            while (!closed) {
                byte[] next = outgoing.poll(250, TimeUnit.MILLISECONDS);
                if (next != null) write(next);
                long now = System.nanoTime();
                if (now - pingAt >= TimeUnit.SECONDS.toNanos(2)) {
                    write(message(9, little(ByteBuffer.allocate(8)).putLong(now).array()));
                    pingAt = now;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (!closed) {
                writerError = "Beim Senden wurde die Verbindung unterbrochen.";
                close();
            }
        }
    }

    private void write(byte[] bytes) throws IOException {
        output.write(bytes);
        output.flush();
    }

    private void authenticate(DataInputStream input, String password)
            throws IOException, GeneralSecurityException {
        byte[] hello = expect(input, 1);
        if (hello.length < 24 || hello[0] != 'R' || hello[1] != 'S' || hello[2] != 'C' || hello[3] != 'R') {
            throw new IOException("Kein gültiger MPC-Remote-Screen-Dienst an dieser Adresse.");
        }
        if (hello[4] != 2 || hello[5] != 0) {
            throw new IOException("Diese Version des MPC-Remote-Screen-Protokolls wird nicht unterstützt.");
        }
        byte[] serverNonce = Arrays.copyOfRange(hello, 8, 24);
        byte[] clientNonce = new byte[16];
        new SecureRandom().nextBytes(clientNonce);
        byte[] key = deriveKey(password);
        try {
            byte[] proof = proof(key, "RSCR-client", serverNonce, clientNonce);
            byte[] auth = ByteBuffer.allocate(50).put(clientNonce).put(proof)
                    .put((byte) SCREEN_CODECS).put((byte) 1).array();
            write(message(2, auth));
            byte[] result = expect(input, 3);
            if (result.length == 0 || result[0] == 0) {
                throw new IOException("Anmeldung abgelehnt. Prüfe das Remote-Screen-Passwort.");
            }
            if (result.length < 33 || ((hello[6] & 1) != 0 && !MessageDigest.isEqual(
                    Arrays.copyOfRange(result, 1, 33), proof(key, "RSCR-server", serverNonce, clientNonce)))) {
                throw new IOException("Die MPC konnte das Passwort nicht bestätigen.");
            }
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    static byte[] deriveKey(String password) throws GeneralSecurityException {
        byte[] utf8 = password.getBytes(StandardCharsets.UTF_8);
        // A single zero byte gives the same padded HMAC key as an empty password.
        Mac mac = hmac(utf8.length == 0 ? new byte[1] : utf8);
        Arrays.fill(utf8, (byte) 0);
        byte[] block = ByteBuffer.allocate(20)
                .put("remote-screen/v1".getBytes(StandardCharsets.US_ASCII)).putInt(1).array();
        byte[] digest = mac.doFinal(block);
        byte[] key = digest.clone();
        for (int i = 1; i < 10000; i++) {
            digest = mac.doFinal(digest);
            for (int j = 0; j < key.length; j++) key[j] ^= digest[j];
        }
        return key;
    }

    static byte[] proof(byte[] key, String label, byte[] server, byte[] client)
            throws GeneralSecurityException {
        Mac mac = hmac(key);
        mac.update(label.getBytes(StandardCharsets.US_ASCII));
        mac.update(server);
        return mac.doFinal(client);
    }

    private static Mac hmac(byte[] key) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac;
    }

    private static byte[] expect(DataInputStream input, int type) throws IOException {
        Message message = receive(input);
        if (message.type != type) throw new IOException("Unerwartete Antwort der MPC.");
        return message.data;
    }

    private static Message receive(DataInputStream input) throws IOException {
        int type = input.readUnsignedByte();
        int length = Integer.reverseBytes(input.readInt());
        int maximum = type == 5 ? MAX_FRAME_MESSAGE : 65536;
        if (type < 1 || type > 16 || length < 0 || length > maximum) {
            throw new IOException("Ungültige Nachricht vom Remote-Screen-Dienst.");
        }
        byte[] data = new byte[length];
        input.readFully(data);
        return new Message(type, data);
    }

    private static byte[] message(int type, byte[] data) {
        return little(ByteBuffer.allocate(5 + data.length)).put((byte) type)
                .putInt(data.length).put(data).array();
    }

    static ByteBuffer little(ByteBuffer bytes) {
        return bytes.order(ByteOrder.LITTLE_ENDIAN);
    }

    private static final class Message {
        final int type;
        final byte[] data;

        Message(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
}
