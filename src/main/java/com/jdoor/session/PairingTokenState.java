package com.jdoor.session;

import com.jdoor.security.SessionToken;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

final class PairingTokenState {
    private final Clock clock;
    private final SecureRandom random;
    private final Duration lifetime;

    private long generation;
    private Credential current;
    private Reservation activeReservation;
    private boolean consumed;
    private boolean rotationPending;

    PairingTokenState(Clock clock, SecureRandom random, Duration lifetime) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
    }

    synchronized Credential rotate() {
        if (activeReservation != null) {
            rotationPending = true;
            return null;
        }
        current = new Credential(
                ++generation, SessionToken.generate(random), clock.instant().plus(lifetime));
        consumed = false;
        rotationPending = false;
        return current;
    }

    synchronized Reservation reserve(String candidate) {
        if (activeReservation != null
                || consumed
                || current == null
                || !clock.instant().isBefore(current.expiresAt())
                || !current.token().constantTimeEquals(candidate)) {
            return null;
        }
        activeReservation = new Reservation(current);
        return activeReservation;
    }

    synchronized boolean consume(Reservation reservation) {
        if (reservation == null
                || activeReservation != reservation
                || current != reservation.credential()
                || current.generation() != reservation.generation()) {
            return false;
        }
        activeReservation = null;
        consumed = true;
        return true;
    }

    synchronized boolean release(Reservation reservation) {
        if (reservation == null || activeReservation != reservation) {
            return false;
        }
        activeReservation = null;
        return rotationPending || current == null || !clock.instant().isBefore(current.expiresAt());
    }

    record Credential(long generation, SessionToken token, Instant expiresAt) {
        Credential {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }

    static final class Reservation {
        private final Credential credential;

        private Reservation(Credential credential) {
            this.credential = credential;
        }

        long generation() {
            return credential.generation();
        }

        private Credential credential() {
            return credential;
        }
    }
}
