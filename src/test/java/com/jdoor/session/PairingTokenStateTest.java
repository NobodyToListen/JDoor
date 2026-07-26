package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PairingTokenStateTest {
    @Test
    void pendingReservationSurvivesRotationAndCannotConsumeTheNextGeneration() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-26T12:00:00Z"), ZoneOffset.UTC);
        PairingTokenState state = new PairingTokenState(clock, new SecureRandom(), Duration.ofMinutes(1));
        PairingTokenState.Credential first = state.rotate();
        PairingTokenState.Reservation reservation = state.reserve(first.token().value());
        assertNotNull(reservation);

        CountDownLatch consumerReady = new CountDownLatch(1);
        CountDownLatch allowConsume = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> consumed = executor.submit(() -> {
                consumerReady.countDown();
                assertTrue(allowConsume.await(5, TimeUnit.SECONDS));
                return state.consume(reservation);
            });

            assertTrue(consumerReady.await(5, TimeUnit.SECONDS));
            clock.advance(Duration.ofMinutes(2));
            assertNull(state.rotate());

            allowConsume.countDown();
            assertTrue(consumed.get(5, TimeUnit.SECONDS));

            PairingTokenState.Credential second = state.rotate();
            assertNotNull(second);
            assertTrue(second.generation() > first.generation());
            assertFalse(state.consume(reservation));
        } finally {
            allowConsume.countDown();
            executor.shutdownNow();
        }
    }
}
