package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PendingHandshakeLimiterTest {
    @Test
    void capsEachAddressBeforeWorkIsQueuedAndReleasesPermitsIdempotently() {
        PendingHandshakeLimiter limiter = new PendingHandshakeLimiter(2);

        PendingHandshakeLimiter.Permit first = limiter.tryAcquire("192.0.2.10");
        PendingHandshakeLimiter.Permit second = limiter.tryAcquire("192.0.2.10");

        assertNotNull(first);
        assertNotNull(second);
        assertNull(limiter.tryAcquire("192.0.2.10"));
        assertNotNull(limiter.tryAcquire("192.0.2.11"));
        assertEquals(2, limiter.pendingFor("192.0.2.10"));

        first.close();
        first.close();
        assertEquals(1, limiter.pendingFor("192.0.2.10"));

        PendingHandshakeLimiter.Permit replacement = limiter.tryAcquire("192.0.2.10");
        assertNotNull(replacement);
        second.close();
        replacement.close();
        assertEquals(0, limiter.pendingFor("192.0.2.10"));
    }
}
