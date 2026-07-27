package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jdoor.security.CertificateFingerprint;
import com.jdoor.security.PairingLink;
import com.jdoor.security.SessionToken;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

class ViewerClientLifecycleTest {
    @Test
    void givesLocalApprovalASeparateHumanScaleTimeout() throws Exception {
        try (SSLSocket socket =
                (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket()) {
            socket.setSoTimeout(ViewerClient.HANDSHAKE_TIMEOUT_MILLIS);

            ViewerClient.configureApprovalWait(socket);

            assertEquals(ViewerClient.APPROVAL_TIMEOUT_MILLIS, socket.getSoTimeout());
            assertTrue(ViewerClient.APPROVAL_TIMEOUT_MILLIS > ViewerClient.HANDSHAKE_TIMEOUT_MILLIS);
        }
    }

    @Test
    void closeAbortsThePublishedCandidateSocketDuringConnect() throws Exception {
        CountDownLatch connectorEntered = new CountDownLatch(1);
        CountDownLatch releaseConnector = new CountDownLatch(1);
        AtomicReference<SSLSocket> candidateSocket = new AtomicReference<>();
        ViewerClient viewer = new ViewerClient(
                pairingLink(), "Lifecycle test", new ViewerEventListener() {}, (socket, address, timeoutMillis) -> {
                    candidateSocket.set(socket);
                    connectorEntered.countDown();
                    try {
                        if (!releaseConnector.await(5, TimeUnit.SECONDS)) {
                            throw new SocketTimeoutException("Test connector timed out");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Test connector was interrupted", interrupted);
                    }
                });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> connection = executor.submit(viewer::connect);

        try {
            assertTrue(connectorEntered.await(5, TimeUnit.SECONDS));

            viewer.close();

            assertTrue(candidateSocket.get().isClosed());
            releaseConnector.countDown();
            ExecutionException failure =
                    assertThrows(ExecutionException.class, () -> connection.get(5, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, failure.getCause());
        } finally {
            releaseConnector.countDown();
            viewer.close();
            executor.shutdownNow();
        }
    }

    @Test
    void aClientClosedBeforeConnectCannotBeReused() {
        ViewerClient viewer = new ViewerClient(pairingLink(), "Lifecycle test", new ViewerEventListener() {});

        viewer.close();

        assertThrows(IllegalStateException.class, viewer::connect);
    }

    private static PairingLink pairingLink() {
        return new PairingLink(
                "127.0.0.1",
                65_535,
                SessionToken.generate(new SecureRandom()),
                new CertificateFingerprint("ab".repeat(32)));
    }
}
