package mpc.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

final class DevicePasswords {
    private static final String APPLICATION = "mpcr-screen";
    private static final String FORMAT = "v1:";

    private DevicePasswords() {}

    static void save(String address, String password) throws IOException, InterruptedException {
        byte[] clear = password.getBytes(StandardCharsets.UTF_8);
        try {
            String encoded = FORMAT + Base64.getEncoder().encodeToString(clear);
            run(encoded, false, "store", "--label=MPCR-Screen saved device",
                    "application", APPLICATION, "address", address);
        } finally {
            Arrays.fill(clear, (byte) 0);
        }
    }

    static String load(String address) throws IOException, InterruptedException {
        String encoded = run(null, true, "lookup", "application", APPLICATION, "address", address);
        if (!encoded.startsWith(FORMAT)) return null;
        try {
            byte[] clear = Base64.getDecoder().decode(encoded.substring(FORMAT.length()));
            try {
                return new String(clear, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(clear, (byte) 0);
            }
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    static void clear() throws IOException, InterruptedException {
        run(null, true, "clear", "application", APPLICATION);
    }

    private static String run(String secret, boolean allowMissing, String... arguments)
            throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "secret-tool";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            try (OutputStream input = process.getOutputStream()) {
                if (secret != null) input.write(secret.getBytes(StandardCharsets.UTF_8));
            }
            if (!process.waitFor(45, TimeUnit.SECONDS)) throw new IOException("Keyring timed out");
            try (InputStream output = process.getInputStream()) {
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = output.read(buffer)) != -1) response.write(buffer, 0, count);
                String text = new String(response.toByteArray(), StandardCharsets.UTF_8).trim();
                int status = process.exitValue();
                if (status == 0 || (allowMissing && status == 1 && text.isEmpty())) return text;
                throw new IOException("System keyring unavailable");
            }
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
