package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jdoor.audit.AuditLog;
import com.jdoor.capture.ScreenSource;
import com.jdoor.control.RemoteInputController;
import com.jdoor.protocol.WireMessage;
import com.jdoor.security.PairingLink;
import com.jdoor.security.SessionToken;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HostViewerIntegrationTest {
    @Test
    void authenticatesStreamsAndKeepsControlOffUntilHostEnablesIt() throws Exception {
        FakeInputController input = new FakeInputController();
        CountDownLatch hostConnected = new CountDownLatch(1);
        CountDownLatch frameReceived = new CountDownLatch(1);
        CountDownLatch controlEnabled = new CountDownLatch(1);
        AtomicReference<Throwable> hostError = new AtomicReference<>();

        HostConfiguration configuration = new HostConfiguration(
                InetAddress.getLoopbackAddress(), "127.0.0.1", 0, 2, 640, 480, 0.6f, Duration.ofMinutes(10));
        HostEventListener hostListener = new HostEventListener() {
            @Override
            public void onViewerConnected(String displayName, String remoteAddress) {
                hostConnected.countDown();
            }

            @Override
            public void onError(String message, Throwable cause) {
                hostError.compareAndSet(null, cause);
            }
        };

        try (HostSessionServer host = new HostSessionServer(
                configuration, new FakeScreenSource(), input, request -> true, hostListener, AuditLog.noOp())) {
            PairingLink link = host.start();

            PairingLink invalidLink = new PairingLink(
                    link.host(), link.port(), SessionToken.generate(new SecureRandom()), link.fingerprint());
            ViewerClient rejected = new ViewerClient(invalidLink, "Invalid viewer", new ViewerEventListener() {});
            assertThrows(ConnectionRejectedException.class, rejected::connect);

            ViewerEventListener viewerListener = new ViewerEventListener() {
                @Override
                public void onScreenFrame(WireMessage.ScreenFrame frame) {
                    frameReceived.countDown();
                }

                @Override
                public void onControlChanged(boolean enabled) {
                    if (enabled) {
                        controlEnabled.countDown();
                    }
                }
            };
            try (ViewerClient viewer = new ViewerClient(link, "Integration viewer", viewerListener)) {
                WireMessage.ServerHello hello = viewer.connect();

                assertEquals(320, hello.screenWidth());
                assertEquals(240, hello.screenHeight());
                assertFalse(hello.controlEnabled());
                assertFalse(viewer.isControlEnabled());
                assertTrue(hostConnected.await(5, TimeUnit.SECONDS));
                assertTrue(frameReceived.await(5, TimeUnit.SECONDS));

                viewer.sendPointer(new WireMessage.PointerInput(WireMessage.PointerAction.MOVE, 0.5f, 0.5f, 0));
                assertFalse(input.pointerReceived.await(150, TimeUnit.MILLISECONDS));

                host.setControlEnabled(true);
                assertTrue(controlEnabled.await(5, TimeUnit.SECONDS));
                viewer.sendPointer(new WireMessage.PointerInput(WireMessage.PointerAction.PRESS, 0.5f, 0.5f, 1));
                viewer.sendKeyboard(new WireMessage.KeyboardInput(WireMessage.KeyAction.PRESS, 65, 0));

                assertTrue(input.pointerReceived.await(5, TimeUnit.SECONDS));
                assertTrue(input.keyboardReceived.await(5, TimeUnit.SECONDS));
                viewer.releaseAllInputs();
                assertTrue(input.releaseReceived.await(5, TimeUnit.SECONDS));
                assertTrue(hostError.get() == null);
            }
        }
    }

    private static final class FakeScreenSource implements ScreenSource {
        private final BufferedImage image;

        private FakeScreenSource() {
            image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.DARK_GRAY);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(20, 20, 120, 80);
            graphics.dispose();
        }

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
        private final CountDownLatch pointerReceived = new CountDownLatch(1);
        private final CountDownLatch keyboardReceived = new CountDownLatch(1);
        private final CountDownLatch releaseReceived = new CountDownLatch(1);

        @Override
        public void apply(WireMessage.PointerInput input) {
            pointerReceived.countDown();
        }

        @Override
        public void apply(WireMessage.KeyboardInput input) {
            keyboardReceived.countDown();
        }

        @Override
        public void releaseAll() {
            releaseReceived.countDown();
        }
    }
}
