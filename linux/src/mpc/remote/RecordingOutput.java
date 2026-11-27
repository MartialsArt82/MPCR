package mpc.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

final class RecordingOutput implements AutoCloseable {
    private static final long FRAME_INTERVAL_NANOS = 1_000_000_000L / 30;

    private final Supplier<ScreenBuffer> screen;
    private final HttpServer server;
    private final ExecutorService worker;
    private final String path = "/" + UUID.randomUUID() + "/";
    private final String origin;
    private ScreenBuffer source;
    private int[] pixels;
    private byte[] rgba;
    private long sourceRevision = -1;
    private long sequence;
    private long nextFrame;

    RecordingOutput(Supplier<ScreenBuffer> screen) throws IOException {
        this.screen = screen;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
        origin = "127.0.0.1:" + server.getAddress().getPort();
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mpcr-obs-output");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(worker);
        server.createContext(path, this::serve);
        server.start();
    }

    String url() {
        return "http://" + origin + path;
    }

    private void serve(HttpExchange exchange) throws IOException {
        try {
            String requestPath = exchange.getRequestURI().getPath();
            if (!"GET".equals(exchange.getRequestMethod())
                    || !origin.equals(exchange.getRequestHeaders().getFirst("Host"))) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            if (path.equals(requestPath)) {
                servePage(exchange);
            } else if ((path + "frame").equals(requestPath)) {
                serveFrame(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        } finally {
            exchange.close();
        }
    }

    private void servePage(HttpExchange exchange) throws IOException {
        try (InputStream page = RecordingOutput.class.getResourceAsStream("/obs.html")) {
            if (page == null) {
                exchange.sendResponseHeaders(500, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, 0);
            byte[] chunk = new byte[4096];
            try (OutputStream stream = exchange.getResponseBody()) {
                int count;
                while ((count = page.read(chunk)) != -1) stream.write(chunk, 0, count);
            }
        }
    }

    private void serveFrame(HttpExchange exchange) throws IOException {
        ScreenBuffer frame = screen.get();
        if (frame == null) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        refresh(frame);
        String tag = "\"" + sequence + "\"";
        exchange.getResponseHeaders().set("ETag", tag);
        if (tag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
            exchange.sendResponseHeaders(304, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.getResponseHeaders().set("X-Frame-Width", Integer.toString(source.width));
        exchange.getResponseHeaders().set("X-Frame-Height", Integer.toString(source.height));
        exchange.sendResponseHeaders(200, rgba.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(rgba);
        }
    }

    private void refresh(ScreenBuffer frame) {
        long now = System.nanoTime();
        if (frame != source) {
            int count = frame.width * frame.height;
            if (pixels == null || pixels.length != count) {
                pixels = new int[count];
                rgba = new byte[count * 4];
            }
            source = frame;
            sourceRevision = -1;
        } else if (now - nextFrame < 0) {
            return;
        }
        nextFrame = now + FRAME_INTERVAL_NANOS;
        long revision = frame.copySnapshot(pixels, sourceRevision);
        if (revision == sourceRevision) return;

        sourceRevision = revision;
        sequence++;
        int offset = 0;
        for (int pixel : pixels) {
            rgba[offset++] = (byte) (pixel >>> 16);
            rgba[offset++] = (byte) (pixel >>> 8);
            rgba[offset++] = (byte) pixel;
            rgba[offset++] = (byte) 255;
        }
    }

    @Override public void close() {
        server.stop(0);
        worker.shutdownNow();
    }
}
