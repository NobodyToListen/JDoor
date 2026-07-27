package com.jdoor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PairingLinkTest {
    private static final CertificateFingerprint FINGERPRINT = new CertificateFingerprint("ab".repeat(32));
    private static final SessionToken TOKEN = SessionToken.generate(new SecureRandom());

    @ParameterizedTest
    @MethodSource("hosts")
    void roundTripsIpv4DnsAndIpv6Hosts(String host) {
        PairingLink original = new PairingLink(host, 8443, TOKEN, FINGERPRINT);

        PairingLink parsed = PairingLink.parse(original.toString());

        assertEquals(original, parsed);
        assertEquals("ABAB-ABAB-ABAB-ABAB", parsed.verificationCode());
    }

    @Test
    void rejectsUnexpectedOrAmbiguousFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse(
                        "https://127.0.0.1:8443/join?token=" + TOKEN.value() + "&fingerprint=" + FINGERPRINT.value()));
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse("jdoor://127.0.0.1:8443/join?token="
                        + TOKEN.value()
                        + "&token="
                        + TOKEN.value()
                        + "&fingerprint="
                        + FINGERPRINT.value()));
        assertThrows(IllegalArgumentException.class, () -> PairingLink.parse("jdoor://127.0.0.1:8443/join?token=x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse("jdoor://user@127.0.0.1:8443/join?token="
                        + TOKEN.value()
                        + "&fingerprint="
                        + FINGERPRINT.value()));
    }

    @Test
    void neverUsesPlainHttp() {
        assertTrue(new PairingLink("localhost", 8443, TOKEN, FINGERPRINT)
                .toString()
                .startsWith("jdoor://"));
    }

    private static Stream<String> hosts() {
        return Stream.of("127.0.0.1", "support-host.local", "::1");
    }
}
