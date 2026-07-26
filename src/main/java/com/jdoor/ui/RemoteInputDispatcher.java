package com.jdoor.ui;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

final class RemoteInputDispatcher implements AutoCloseable {
    static final int DEFAULT_MAX_PENDING = 64;

    private final Object monitor = new Object();
    private final ArrayDeque<PendingInput> pendingInputs = new ArrayDeque<>();
    private final int maxPending;
    private final InputAction overflowRelease;
    private final Consumer<Exception> failureHandler;
    private final Thread worker;

    private boolean closed;

    RemoteInputDispatcher(InputAction overflowRelease, Consumer<Exception> failureHandler) {
        this(DEFAULT_MAX_PENDING, overflowRelease, failureHandler);
    }

    RemoteInputDispatcher(int maxPending, InputAction overflowRelease, Consumer<Exception> failureHandler) {
        if (maxPending < 1) {
            throw new IllegalArgumentException("maxPending must be positive");
        }
        this.maxPending = maxPending;
        this.overflowRelease = Objects.requireNonNull(overflowRelease, "overflowRelease");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        worker = Thread.ofPlatform().daemon().name("jdoor-viewer-input").unstarted(this::runLoop);
        worker.start();
    }

    void submitMove(InputAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (monitor) {
            if (closed || hasPendingRelease()) {
                return;
            }
            removePendingMove();
            if (pendingInputs.size() < maxPending) {
                pendingInputs.addLast(new PendingInput(InputKind.MOVE, action));
                monitor.notifyAll();
            }
        }
    }

    void submitDiscrete(InputAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (monitor) {
            if (closed || hasPendingRelease()) {
                return;
            }
            if (pendingInputs.size() >= maxPending) {
                enqueueRelease(overflowRelease);
                return;
            }
            pendingInputs.addLast(new PendingInput(InputKind.DISCRETE, action));
            monitor.notifyAll();
        }
    }

    void submitReleaseAll(InputAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (monitor) {
            if (!closed) {
                enqueueRelease(action);
            }
        }
    }

    void clearPending() {
        synchronized (monitor) {
            pendingInputs.clear();
        }
    }

    int pendingCount() {
        synchronized (monitor) {
            return pendingInputs.size();
        }
    }

    private void runLoop() {
        while (true) {
            PendingInput input;
            synchronized (monitor) {
                while (!closed && pendingInputs.isEmpty()) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (closed) {
                    return;
                }
                input = pendingInputs.removeFirst();
            }
            try {
                input.action().run();
            } catch (IOException | RuntimeException failure) {
                failureHandler.accept(failure);
            }
        }
    }

    private void enqueueRelease(InputAction action) {
        pendingInputs.clear();
        pendingInputs.addFirst(new PendingInput(InputKind.RELEASE_ALL, action));
        monitor.notifyAll();
    }

    private boolean hasPendingRelease() {
        return !pendingInputs.isEmpty() && pendingInputs.getFirst().kind() == InputKind.RELEASE_ALL;
    }

    private void removePendingMove() {
        Iterator<PendingInput> iterator = pendingInputs.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().kind() == InputKind.MOVE) {
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public void close() {
        synchronized (monitor) {
            if (closed) {
                return;
            }
            closed = true;
            pendingInputs.clear();
            monitor.notifyAll();
        }
        worker.interrupt();
    }

    @FunctionalInterface
    interface InputAction {
        void run() throws IOException;
    }

    private enum InputKind {
        MOVE,
        DISCRETE,
        RELEASE_ALL
    }

    private record PendingInput(InputKind kind, InputAction action) {}
}
