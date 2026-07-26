package com.jdoor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RemoteInputDispatcherTest {
    @Test
    void keepsOnlyTheLatestPendingMove() throws Exception {
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch unblock = new CountDownLatch(1);
        CountDownLatch moveDelivered = new CountDownLatch(1);
        AtomicInteger deliveredMove = new AtomicInteger(-1);

        try (RemoteInputDispatcher dispatcher = new RemoteInputDispatcher(() -> {}, ignored -> {})) {
            dispatcher.submitDiscrete(blockingAction(blockerStarted, unblock));
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));

            for (int sequence = 0; sequence < 100; sequence++) {
                int currentSequence = sequence;
                dispatcher.submitMove(() -> {
                    deliveredMove.set(currentSequence);
                    moveDelivered.countDown();
                });
            }

            assertEquals(1, dispatcher.pendingCount());
            unblock.countDown();
            assertTrue(moveDelivered.await(5, TimeUnit.SECONDS));
            assertEquals(99, deliveredMove.get());
        }
    }

    @Test
    void releaseAllClearsAndTakesPriorityOverPendingInput() throws Exception {
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch unblock = new CountDownLatch(1);
        CountDownLatch releaseDelivered = new CountDownLatch(1);
        AtomicInteger staleInputs = new AtomicInteger();

        try (RemoteInputDispatcher dispatcher = new RemoteInputDispatcher(() -> {}, ignored -> {})) {
            dispatcher.submitDiscrete(blockingAction(blockerStarted, unblock));
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            dispatcher.submitDiscrete(staleInputs::incrementAndGet);
            dispatcher.submitMove(staleInputs::incrementAndGet);

            dispatcher.submitReleaseAll(releaseDelivered::countDown);

            assertEquals(1, dispatcher.pendingCount());
            unblock.countDown();
            assertTrue(releaseDelivered.await(5, TimeUnit.SECONDS));
            assertEquals(0, staleInputs.get());
        }
    }

    @Test
    void overflowReplacesTheBoundedBacklogWithAnEmergencyRelease() throws Exception {
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch unblock = new CountDownLatch(1);
        CountDownLatch emergencyRelease = new CountDownLatch(1);
        AtomicInteger staleInputs = new AtomicInteger();

        try (RemoteInputDispatcher dispatcher =
                new RemoteInputDispatcher(3, emergencyRelease::countDown, ignored -> {})) {
            dispatcher.submitDiscrete(blockingAction(blockerStarted, unblock));
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            dispatcher.submitDiscrete(staleInputs::incrementAndGet);
            dispatcher.submitDiscrete(staleInputs::incrementAndGet);
            dispatcher.submitDiscrete(staleInputs::incrementAndGet);

            dispatcher.submitDiscrete(staleInputs::incrementAndGet);

            assertEquals(1, dispatcher.pendingCount());
            unblock.countDown();
            assertTrue(emergencyRelease.await(5, TimeUnit.SECONDS));
            assertEquals(0, staleInputs.get());
        }
    }

    private static RemoteInputDispatcher.InputAction blockingAction(CountDownLatch started, CountDownLatch unblock) {
        return () -> {
            started.countDown();
            try {
                if (!unblock.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out waiting to release the test dispatcher");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Test dispatcher was interrupted", interrupted);
            }
        };
    }
}
