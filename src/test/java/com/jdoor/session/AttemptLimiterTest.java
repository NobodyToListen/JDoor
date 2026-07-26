package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AttemptLimiterTest {
    @Test
    void blocksAtThresholdAndRecoversAfterWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-26T12:00:00Z"), ZoneOffset.UTC);
        AttemptLimiter limiter = new AttemptLimiter(3, Duration.ofMinutes(1), clock);

        limiter.registerFailure("192.0.2.1");
        limiter.registerFailure("192.0.2.1");
        assertFalse(limiter.isBlocked("192.0.2.1"));
        limiter.registerFailure("192.0.2.1");
        assertTrue(limiter.isBlocked("192.0.2.1"));

        clock.advance(Duration.ofMinutes(2));
        assertFalse(limiter.isBlocked("192.0.2.1"));
        assertTrue(limiter.trackedAddressCount() == 0);
    }

    @Test
    void successClearsFailuresForThatAddressOnly() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-26T12:00:00Z"), ZoneOffset.UTC);
        AttemptLimiter limiter = new AttemptLimiter(1, Duration.ofMinutes(1), clock);

        limiter.registerFailure("192.0.2.1");
        limiter.registerFailure("192.0.2.2");
        limiter.registerSuccess("192.0.2.1");

        assertFalse(limiter.isBlocked("192.0.2.1"));
        assertTrue(limiter.isBlocked("192.0.2.2"));
    }
}
