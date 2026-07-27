package com.jdoor.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SessionTokenTest {
    @Test
    void generatedTokensAre128BitUrlSafeAndRedacted() {
        SessionToken first = SessionToken.generate(new SecureRandom());
        SessionToken second = SessionToken.generate(new SecureRandom());

        assertNotEquals(first.value(), second.value());
        assertTrue(first.value().matches("[A-Za-z0-9_-]{22}"));
        assertTrue(first.constantTimeEquals(first.value()));
        assertFalse(first.constantTimeEquals(second.value()));
        assertFalse(first.constantTimeEquals(null));
        assertNotEquals(first.value(), first.toString());
    }

    @Test
    void rejectsMalformedOrWrongLengthTokens() {
        assertThrows(IllegalArgumentException.class, () -> new SessionToken("not base64!"));
        assertThrows(IllegalArgumentException.class, () -> new SessionToken("c2hvcnQ"));
        assertThrows(IllegalArgumentException.class, () -> new SessionToken("AAAAAAAAAAAAAAAAAAAAAA=="));
    }
}
