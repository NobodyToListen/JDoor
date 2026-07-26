package com.jdoor.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record PairingLink(String host, int port, SessionToken token, CertificateFingerprint fingerprint) {
    public static final String SCHEME = "jdoor";

    public PairingLink {
        host = Objects.requireNonNull(host, "host").strip();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        token = Objects.requireNonNull(token, "token");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (host.isEmpty() || host.length() > 253 || host.contains("/") || host.contains("@")) {
            throw new IllegalArgumentException("Host is invalid");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }

    public static PairingLink parse(String rawLink) {
        Objects.requireNonNull(rawLink, "rawLink");
        try {
            URI uri = new URI(rawLink.strip());
            if (!SCHEME.equals(uri.getScheme())
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null
                    || !"/join".equals(uri.getPath())) {
                throw new IllegalArgumentException("Expected a jdoor://…/join pairing link");
            }
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Pairing link does not contain a valid host");
            }
            int port = uri.getPort();
            Map<String, String> query = parseQuery(uri.getRawQuery());
            if (query.size() != 2 || !query.containsKey("token") || !query.containsKey("fingerprint")) {
                throw new IllegalArgumentException("Pairing link must contain token and fingerprint only");
            }
            return new PairingLink(
                    host,
                    port,
                    new SessionToken(query.get("token")),
                    new CertificateFingerprint(query.get("fingerprint")));
        } catch (URISyntaxException invalidUri) {
            throw new IllegalArgumentException("Pairing link is malformed", invalidUri);
        }
    }

    public URI toUri() {
        try {
            String query = "token=" + token.value() + "&fingerprint=" + fingerprint.value();
            return new URI(SCHEME, null, host, port, "/join", query, null);
        } catch (URISyntaxException impossible) {
            throw new IllegalStateException("Validated pairing link could not be encoded", impossible);
        }
    }

    public String verificationCode() {
        return fingerprint.grouped().toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return toUri().toASCIIString();
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&", -1)) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Pairing link query is malformed");
            }
            if (!parts[0].matches("[a-z]+") || !parts[1].matches("[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException("Pairing link contains unsupported characters");
            }
            if (values.putIfAbsent(parts[0], parts[1]) != null) {
                throw new IllegalArgumentException("Pairing link contains duplicate fields");
            }
        }
        return Map.copyOf(values);
    }
}
