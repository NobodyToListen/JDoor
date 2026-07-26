package com.jdoor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

class TlsSocketPolicyTest {
    @Test
    void clientRequiresEndpointIdentification() throws Exception {
        try (SSLSocket socket = TlsSocketPolicy.createClient(SSLContext.getDefault())) {
            assertEquals("HTTPS", socket.getSSLParameters().getEndpointIdentificationAlgorithm());
        }
    }
}
