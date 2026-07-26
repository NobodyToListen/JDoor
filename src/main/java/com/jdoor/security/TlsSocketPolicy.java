package com.jdoor.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

public final class TlsSocketPolicy {
    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("TLSv1.3", "TLSv1.2");

    private TlsSocketPolicy() {}

    public static void apply(SSLSocket socket) {
        String[] enabled = Arrays.stream(socket.getSupportedProtocols())
                .filter(ALLOWED_PROTOCOLS::contains)
                .toArray(String[]::new);
        if (enabled.length == 0) {
            throw new IllegalStateException("This JVM does not support TLS 1.2 or newer");
        }
        socket.setEnabledProtocols(enabled);
        socket.setUseClientMode(false);
        socket.setNeedClientAuth(false);
    }

    public static void applyClient(SSLSocket socket) {
        Set<String> supported = Arrays.stream(socket.getSupportedProtocols()).collect(Collectors.toUnmodifiableSet());
        String[] enabled =
                ALLOWED_PROTOCOLS.stream().filter(supported::contains).toArray(String[]::new);
        if (enabled.length == 0) {
            throw new IllegalStateException("This JVM does not support TLS 1.2 or newer");
        }
        socket.setEnabledProtocols(enabled);
        socket.setUseClientMode(true);
        SSLParameters parameters = socket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parameters);
    }
}
