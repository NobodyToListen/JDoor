package com.jdoor.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AttemptLimiter {
    private final int maximumFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    AttemptLimiter(int maximumFailures, Duration window, Clock clock) {
        this.maximumFailures = maximumFailures;
        this.window = window;
        this.clock = clock;
    }

    boolean isBlocked(String remoteAddress) {
        while (true) {
            Deque<Instant> attempts = failures.get(remoteAddress);
            if (attempts == null) {
                return false;
            }
            synchronized (attempts) {
                if (failures.get(remoteAddress) != attempts) {
                    continue;
                }
                discardExpired(attempts);
                if (attempts.isEmpty()) {
                    failures.remove(remoteAddress, attempts);
                    return false;
                }
                return attempts.size() >= maximumFailures;
            }
        }
    }

    void registerFailure(String remoteAddress) {
        while (true) {
            Deque<Instant> attempts = failures.computeIfAbsent(remoteAddress, ignored -> new ArrayDeque<>());
            synchronized (attempts) {
                if (failures.get(remoteAddress) != attempts) {
                    continue;
                }
                discardExpired(attempts);
                attempts.addLast(clock.instant());
                return;
            }
        }
    }

    void registerSuccess(String remoteAddress) {
        failures.remove(remoteAddress);
    }

    int trackedAddressCount() {
        return failures.size();
    }

    private void discardExpired(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.removeFirst();
        }
    }
}
