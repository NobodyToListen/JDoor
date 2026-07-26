package com.jdoor.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public record SessionToken(String value) {
    private static final int TOKEN_BYTES = 16;

    public SessionToken {
        value = Objects.requireNonNull(value, "value");
        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException invalidBase64) {
            throw new IllegalArgumentException("Session token is not valid base64url", invalidBase64);
        }
        if (decoded.length != TOKEN_BYTES || value.contains("=")) {
            throw new IllegalArgumentException("Session token must contain 128 bits without padding");
        }
    }

    public static SessionToken generate(SecureRandom random) {
        Objects.requireNonNull(random, "random");
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return new SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public boolean constantTimeEquals(String candidate) {
        if (candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                value.getBytes(StandardCharsets.US_ASCII), candidate.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public String toString() {
        return "[redacted]";
    }
}
