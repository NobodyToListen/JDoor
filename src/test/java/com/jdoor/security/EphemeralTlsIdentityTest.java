package com.jdoor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EphemeralTlsIdentityTest {
    @Test
    void createsShortLivedPinnedServerIdentity() throws Exception {
        Instant now = Instant.parse("2026-07-26T12:00:00Z");
        EphemeralTlsIdentity identity =
                EphemeralTlsIdentity.create(Clock.fixed(now, ZoneOffset.UTC), new SecureRandom());

        assertNotNull(identity.serverContext());
        assertTrue(identity.fingerprint().matches(identity.certificate()));
        assertEquals(Duration.ofHours(24), Duration.between(now, identity.expiresAt()));
        assertEquals(-1, identity.certificate().getBasicConstraints());
        assertTrue(identity.certificate().getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.1"));
    }
}
