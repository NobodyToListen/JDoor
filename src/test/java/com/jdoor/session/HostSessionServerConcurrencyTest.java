package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jdoor.audit.AuditLog;
import com.jdoor.capture.ScreenSource;
import com.jdoor.control.RemoteInputController;
import com.jdoor.protocol.MessageChannel;
import com.jdoor.protocol.WireMessage;
import com.jdoor.security.PairingLink;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

class HostSessionServerConcurrencyTest {
    @Test
    void delayedControlCommitCannotMigrateFromConnectionAToConnectionB() throws Exception {
        CountDownLatch controlCommitCaptured = new CountDownLatch(1);
        CountDownLatch allowControlCommit = new CountDownLatch(1);
        AtomicBoolean blockFirstCommit = new AtomicBoolean(true);
        List<String> lifecycleEvents = new CopyOnWriteArrayList<>();
        FakeInputController input = new FakeInputController();
        ExecutorService controlExecutor = Executors.newSingleThreadExecutor();

        HostEventListener listener = new HostEventListener() {
            @Override
            public void onViewerConnected(String displayName, String remoteAddress) {
                lifecycleEvents.add("connected:" + displayName);
            }

            @Override
            public void onViewerDisconnected(String reason) {
                lifecycleEvents.add("disconnected");
            }
        };
        HostSessionServer.ControlCommitHook hook = ignored -> {
            if (blockFirstCommit.compareAndSet(true, false)) {
                controlCommitCaptured.countDown();
                awaitUnchecked(allowControlCommit);
            }
        };

        try (HostSessionServer host = new HostSessionServer(
                configuration(),
                new FakeScreenSource(),
                input,
                request -> true,
                listener,
                AuditLog.noOp(),
                Clock.systemUTC(),
                new SecureRandom(),
                hook)) {
            PairingLink firstLink = host.start();
            try (ViewerClient firstViewer = new ViewerClient(firstLink, "Viewer A", new ViewerEventListener() {})) {
                firstViewer.connect();
                awaitCondition(() -> lifecycleEvents.contains("connected:Viewer A"));

                Future<?> delayedEnable = controlExecutor.submit(() -> host.setControlEnabled(true));
                assertTrue(controlCommitCaptured.await(5, TimeUnit.SECONDS));

                host.disconnectViewer();
                PairingLink secondLink = host.pairingLink();
                try (ViewerClient secondViewer =
                        new ViewerClient(secondLink, "Viewer B", new ViewerEventListener() {})) {
                    secondViewer.connect();
                    awaitCondition(() -> lifecycleEvents.contains("connected:Viewer B"));

                    allowControlCommit.countDown();
                    delayedEnable.get(5, TimeUnit.SECONDS);

                    assertFalse(host.isControlEnabled());
                    assertFalse(secondViewer.isControlEnabled());
                    assertEquals(List.of("connected:Viewer A", "disconnected", "connected:Viewer B"), lifecycleEvents);
                    assertEquals(0, input.pointerCount.get());
                }
            }
        } finally {
            allowControlCommit.countDown();
            controlExecutor.shutdownNow();
        }
    }

    @Test
    void disconnectCompletesWhileAFrameSendOwnsTheChannelWriteLock() throws Exception {
        AtomicReference<BlockingFrameChannel> openedChannel = new AtomicReference<>();
        FakeInputController input = new FakeInputController();
        HostSessionServer.ChannelFactory channelFactory = socket -> {
            BlockingFrameChannel channel = new BlockingFrameChannel(socket);
            openedChannel.set(channel);
            return channel;
        };

        try (HostSessionServer host = new HostSessionServer(
                configuration(),
                new FakeScreenSource(),
                input,
                request -> true,
                new HostEventListener() {},
                AuditLog.noOp(),
                Clock.systemUTC(),
                new SecureRandom(),
                channelFactory,
                ignored -> {})) {
            PairingLink link = host.start();
            try (ViewerClient viewer = new ViewerClient(link, "Blocked writer", new ViewerEventListener() {})) {
                viewer.connect();
                BlockingFrameChannel channel = awaitChannel(openedChannel);
                assertTrue(channel.frameSendStarted.await(5, TimeUnit.SECONDS));

                assertTimeoutPreemptively(Duration.ofSeconds(2), host::disconnectViewer);

                assertFalse(host.hasViewer());
                assertTrue(channel.closed.await(2, TimeUnit.SECONDS));
                assertTrue(input.releaseReceived.await(2, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void closeTerminatesAnInFlightTlsHandshakeWithinTheDeadline() throws Exception {
        HostSessionServer host = new HostSessionServer(
                configuration(),
                new FakeScreenSource(),
                new FakeInputController(),
                request -> true,
                new HostEventListener() {},
                AuditLog.noOp());
        try {
            PairingLink link = host.start();
            try (Socket slowClient = new Socket(link.host(), link.port())) {
                awaitCondition(() -> host.pendingCandidateCount() == 1);

                assertTimeoutPreemptively(Duration.ofSeconds(2), host::close);
                assertFalse(host.hasViewer());

                slowClient.setSoTimeout(2_000);
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    try {
                        while (slowClient.getInputStream().read() != -1) {
                            // A TLS alert may precede EOF; drain it to prove the socket is closed.
                        }
                    } catch (SocketException expected) {
                        assertTrue(slowClient.isConnected());
                    }
                });
            }
        } finally {
            host.close();
        }
    }

    @Test
    void absolutePreAuthenticationDeadlineClosesSlowHandshakeAndReleasesPermit() throws Exception {
        HostSessionServer host = new HostSessionServer(
                configuration(),
                new FakeScreenSource(),
                new FakeInputController(),
                request -> true,
                new HostEventListener() {},
                AuditLog.noOp(),
                Clock.systemUTC(),
                new SecureRandom(),
                Duration.ofMillis(250));
        try {
            PairingLink link = host.start();
            try (Socket slowClient = new Socket(link.host(), link.port())) {
                awaitCondition(() -> host.pendingHandshakeCount("127.0.0.1") == 1);
                awaitCondition(() -> host.pendingCandidateCount() == 0);

                assertEquals(0, host.pendingHandshakeCount("127.0.0.1"));
                slowClient.setSoTimeout(2_000);
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    try {
                        while (slowClient.getInputStream().read() != -1) {
                            // A TLS alert may precede EOF; drain it to prove the socket is closed.
                        }
                    } catch (SocketException expected) {
                        assertTrue(slowClient.isConnected());
                    }
                });
            }
        } finally {
            host.close();
        }
    }

    private static HostConfiguration configuration() throws Exception {
        return new HostConfiguration(
                InetAddress.getLoopbackAddress(), "127.0.0.1", 0, 15, 320, 240, 0.6f, Duration.ofMinutes(10));
    }

    private static BlockingFrameChannel awaitChannel(AtomicReference<BlockingFrameChannel> channelReference)
            throws Exception {
        awaitCondition(() -> channelReference.get() != null);
        return channelReference.get();
    }

    private static void awaitCondition(CheckedCondition condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.evaluate()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Condition did not become true before the deadline");
            }
            Thread.sleep(10);
        }
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Latch was not released before the deadline");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while awaiting the test seam", interrupted);
        }
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean evaluate() throws Exception;
    }

    private static final class FakeScreenSource implements ScreenSource {
        private final BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);

        @Override
        public Rectangle bounds() {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        @Override
        public BufferedImage capture() {
            return image;
        }
    }

    private static final class FakeInputController implements RemoteInputController {
        private final AtomicInteger pointerCount = new AtomicInteger();
        private final CountDownLatch releaseReceived = new CountDownLatch(1);

        @Override
        public void apply(WireMessage.PointerInput input) {
            pointerCount.incrementAndGet();
        }

        @Override
        public void apply(WireMessage.KeyboardInput input) {}

        @Override
        public void releaseAll() {
            releaseReceived.countDown();
        }
    }

    private static final class BlockingFrameChannel implements HostSessionServer.SessionChannel {
        private final MessageChannel delegate;
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final CountDownLatch frameSendStarted = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        private BlockingFrameChannel(SSLSocket socket) throws IOException {
            delegate = new MessageChannel(socket);
        }

        @Override
        public WireMessage read() throws IOException {
            return delegate.read();
        }

        @Override
        public synchronized void send(WireMessage message) throws IOException {
            if (message instanceof WireMessage.ScreenFrame) {
                frameSendStarted.countDown();
                try {
                    if (!closed.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting for the channel to close");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while blocking the frame write", interrupted);
                }
                throw new IOException("Channel closed during frame write");
            }
            delegate.send(message);
        }

        @Override
        public boolean isOpen() {
            return open.get() && delegate.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (open.compareAndSet(true, false)) {
                closed.countDown();
                delegate.close();
            }
        }
    }
}
