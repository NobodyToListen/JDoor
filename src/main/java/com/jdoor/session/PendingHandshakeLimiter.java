package com.jdoor.session;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class PendingHandshakeLimiter {
    private final int maximumPendingPerAddress;
    private final ConcurrentMap<String, Integer> pending = new ConcurrentHashMap<>();

    PendingHandshakeLimiter(int maximumPendingPerAddress) {
        if (maximumPendingPerAddress < 1) {
            throw new IllegalArgumentException("maximumPendingPerAddress must be positive");
        }
        this.maximumPendingPerAddress = maximumPendingPerAddress;
    }

    Permit tryAcquire(String remoteAddress) {
        Objects.requireNonNull(remoteAddress, "remoteAddress");
        AtomicReference<Permit> acquired = new AtomicReference<>();
        pending.compute(remoteAddress, (ignored, current) -> {
            int count = current == null ? 0 : current;
            if (count >= maximumPendingPerAddress) {
                return current;
            }
            acquired.set(new Permit(this, remoteAddress));
            return count + 1;
        });
        return acquired.get();
    }

    int pendingFor(String remoteAddress) {
        return pending.getOrDefault(remoteAddress, 0);
    }

    private void release(String remoteAddress) {
        pending.computeIfPresent(remoteAddress, (ignored, current) -> current == 1 ? null : current - 1);
    }

    static final class Permit implements AutoCloseable {
        private final PendingHandshakeLimiter owner;
        private final String remoteAddress;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(PendingHandshakeLimiter owner, String remoteAddress) {
            this.owner = owner;
            this.remoteAddress = remoteAddress;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.release(remoteAddress);
            }
        }
    }
}
