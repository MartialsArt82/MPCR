package mpc.remote;

final class Endpoint {
    final String host;
    final int port;

    private Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    static Endpoint parse(String address) {
        String host = address.trim();
        String portText = null;
        if (host.startsWith("[")) {
            int closing = host.indexOf(']');
            if (closing < 0) throw invalid();
            String suffix = host.substring(closing + 1);
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":")) throw invalid();
                portText = suffix.substring(1);
            }
            host = host.substring(1, closing);
        } else {
            int separator = host.indexOf(':');
            if (separator >= 0 && separator == host.lastIndexOf(':')) {
                portText = host.substring(separator + 1);
                host = host.substring(0, separator);
            }
        }
        if (host.isEmpty() || host.matches(".*[\\s/\\[\\]].*")) throw invalid();
        int port = RemoteSession.DEFAULT_PORT;
        if (portText != null) {
            if (!portText.matches("[0-9]{1,5}")) throw invalid();
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw invalid();
        }
        return new Endpoint(host, port);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Gib die IP-Adresse oder den Hostnamen der MPC ein.");
    }
}
