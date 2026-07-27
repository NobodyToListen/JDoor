package com.jdoor.session;

import com.jdoor.protocol.MessageChannel;
import com.jdoor.protocol.ProtocolException;
import com.jdoor.protocol.WireMessage;
import com.jdoor.security.PairingLink;
import com.jdoor.security.PinnedTlsContext;
import com.jdoor.security.TlsSocketPolicy;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public final class ViewerClient implements AutoCloseable {
    static final int HANDSHAKE_TIMEOUT_MILLIS = 15_000;
    static final int APPROVAL_TIMEOUT_MILLIS = 120_000;
    static final int SESSION_TIMEOUT_MILLIS = 30_000;

    private final PairingLink pairingLink;
    private final String displayName;
    private final ViewerEventListener listener;
    private final SocketConnector socketConnector;
    private final ExecutorService readerExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean controlEnabled = new AtomicBoolean();
    private final Object lifecycleMonitor = new Object();

    private volatile SSLSocket socket;
    private volatile MessageChannel channel;
    private volatile ScheduledFuture<?> heartbeatTask;

    public ViewerClient(PairingLink pairingLink, String displayName, ViewerEventListener listener) {
        this(
                pairingLink,
                displayName,
                listener,
                (socket, address, timeoutMillis) -> socket.connect(address, timeoutMillis));
    }

    ViewerClient(
            PairingLink pairingLink,
            String displayName,
            ViewerEventListener listener,
            SocketConnector socketConnector) {
        this.pairingLink = Objects.requireNonNull(pairingLink, "pairingLink");
        this.displayName = Objects.requireNonNull(displayName, "displayName").strip();
        this.listener = Objects.requireNonNull(listener, "listener");
        this.socketConnector = Objects.requireNonNull(socketConnector, "socketConnector");
        readerExecutor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().daemon().name("jdoor-viewer-reader").factory());
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("jdoor-viewer-heartbeat").factory());
    }

    public WireMessage.ServerHello connect() throws IOException {
        if (closed.get() || connected.get() || socket != null) {
            throw new IllegalStateException("This viewer client has already been used");
        }
        SSLContext context = PinnedTlsContext.create(pairingLink.fingerprint());
        SSLSocket candidate = TlsSocketPolicy.createClient(context);
        publishCandidate(candidate);
        try {
            ensureOpenDuringConnect();
            socketConnector.connect(candidate, new InetSocketAddress(pairingLink.host(), pairingLink.port()), 8_000);
            ensureOpenDuringConnect();
            candidate.setTcpNoDelay(true);
            candidate.setKeepAlive(true);
            candidate.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
            candidate.startHandshake();
            ensureOpenDuringConnect();

            MessageChannel candidateChannel = new MessageChannel(candidate);
            channel = candidateChannel;
            ensureOpenDuringConnect();
            candidateChannel.send(
                    new WireMessage.ClientHello(pairingLink.token().value(), displayName));
            configureApprovalWait(candidate);
            WireMessage response = candidateChannel.read();
            if (response instanceof WireMessage.Rejected rejected) {
                candidateChannel.close();
                throw new ConnectionRejectedException(rejected.reason());
            }
            if (!(response instanceof WireMessage.ServerHello hello)) {
                candidateChannel.close();
                throw new ProtocolException("Host did not send a valid session response");
            }

            ensureOpenDuringConnect();
            candidate.setSoTimeout(SESSION_TIMEOUT_MILLIS);
            controlEnabled.set(hello.controlEnabled());
            connected.set(true);
            ensureOpenDuringConnect();
            safely(() -> listener.onConnected(hello));
            safely(() -> listener.onControlChanged(hello.controlEnabled()));
            readerExecutor.execute(this::readLoop);
            heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(this::sendHeartbeat, 10, 10, TimeUnit.SECONDS);
            return hello;
        } catch (IOException | RuntimeException failure) {
            terminate("Connection failed", null);
            throw failure;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isControlEnabled() {
        return controlEnabled.get();
    }

    public void sendPointer(WireMessage.PointerInput input) throws IOException {
        if (connected.get() && controlEnabled.get()) {
            channel.send(input);
        }
    }

    public void sendKeyboard(WireMessage.KeyboardInput input) throws IOException {
        if (connected.get() && controlEnabled.get()) {
            channel.send(input);
        }
    }

    public void releaseAllInputs() throws IOException {
        if (connected.get()) {
            channel.send(new WireMessage.ReleaseAllInputs());
        }
    }

    public void disconnect() {
        terminate("Viewer ended the session", null);
    }

    private void readLoop() {
        String reason = "Connection closed";
        Throwable error = null;
        try {
            while (connected.get() && channel.isOpen()) {
                WireMessage message = channel.read();
                if (message instanceof WireMessage.ScreenFrame frame) {
                    safely(() -> listener.onScreenFrame(frame));
                } else if (message instanceof WireMessage.ControlState state) {
                    controlEnabled.set(state.enabled());
                    safely(() -> listener.onControlChanged(state.enabled()));
                } else if (message instanceof WireMessage.Ping ping) {
                    channel.send(new WireMessage.Pong(ping.nonce()));
                } else if (message instanceof WireMessage.Pong) {
                    // A successful read is enough to confirm liveness.
                } else if (message instanceof WireMessage.Goodbye goodbye) {
                    reason = goodbye.reason();
                    break;
                } else {
                    throw new ProtocolException("Host sent a message that is not allowed in this direction");
                }
            }
        } catch (SocketTimeoutException timeout) {
            reason = "Host stopped responding";
            error = timeout;
        } catch (EOFException | SocketException disconnected) {
            reason = "Connection closed";
        } catch (Exception failure) {
            reason = "Session failed";
            error = failure;
        } finally {
            terminate(reason, error);
        }
    }

    private void sendHeartbeat() {
        if (!connected.get()) {
            return;
        }
        try {
            channel.send(new WireMessage.Ping(System.nanoTime()));
        } catch (IOException failure) {
            terminate("Host stopped responding", failure);
        }
    }

    private void terminate(String reason, Throwable failure) {
        boolean firstClose = closed.compareAndSet(false, true);
        boolean wasConnected = connected.getAndSet(false);
        if (!firstClose) {
            closeUnusedResources();
            shutdownExecutors();
            return;
        }
        controlEnabled.set(false);
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(false);
        }
        closeUnusedResources();
        if (wasConnected) {
            if (failure != null) {
                safely(() -> listener.onError(reason, failure));
            }
            safely(() -> listener.onControlChanged(false));
            safely(() -> listener.onDisconnected(reason));
        }
        shutdownExecutors();
    }

    private void publishCandidate(SSLSocket candidate) throws IOException {
        synchronized (lifecycleMonitor) {
            if (closed.get() || socket != null) {
                candidate.close();
                throw new IllegalStateException("This viewer client has already been used");
            }
            socket = candidate;
        }
        ensureOpenDuringConnect();
    }

    private void ensureOpenDuringConnect() throws SocketException {
        if (closed.get()) {
            throw new SocketException("Viewer connection was cancelled");
        }
    }

    static void configureApprovalWait(SSLSocket candidate) throws SocketException {
        candidate.setSoTimeout(APPROVAL_TIMEOUT_MILLIS);
    }

    private void shutdownExecutors() {
        heartbeatExecutor.shutdownNow();
        readerExecutor.shutdownNow();
    }

    private void closeUnusedResources() {
        MessageChannel currentChannel = channel;
        if (currentChannel != null) {
            try {
                currentChannel.close();
            } catch (IOException ignored) {
                // Closing is best-effort.
            }
            return;
        }
        SSLSocket currentSocket = socket;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
                // Closing is best-effort.
            }
        }
    }

    private static void safely(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException ignored) {
            // A presentation-layer callback must not stop a network session.
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    @FunctionalInterface
    interface SocketConnector {
        void connect(SSLSocket socket, InetSocketAddress address, int timeoutMillis) throws IOException;
    }
}
