package com.jdoor.session;

import com.jdoor.audit.AuditEvent;
import com.jdoor.audit.AuditLog;
import com.jdoor.capture.ScreenEncoder;
import com.jdoor.capture.ScreenSource;
import com.jdoor.control.RemoteInputController;
import com.jdoor.protocol.MessageChannel;
import com.jdoor.protocol.ProtocolException;
import com.jdoor.protocol.WireMessage;
import com.jdoor.security.EphemeralTlsIdentity;
import com.jdoor.security.PairingLink;
import com.jdoor.security.TlsSocketPolicy;
import java.awt.Rectangle;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public final class HostSessionServer implements AutoCloseable {
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(1);
    private static final Duration PRE_AUTH_NOTICE_INTERVAL = Duration.ofSeconds(30);
    private static final Duration PRE_AUTH_DEADLINE = Duration.ofSeconds(10);
    private static final int MAXIMUM_FAILED_ATTEMPTS = 5;
    private static final int MAXIMUM_PENDING_HANDSHAKES_PER_ADDRESS = 2;
    private static final int TLS_HANDSHAKE_TIMEOUT_MILLIS = 5_000;
    private static final int CLIENT_HELLO_TIMEOUT_MILLIS = 5_000;
    private static final int SESSION_READ_TIMEOUT_MILLIS = 30_000;

    private final HostConfiguration configuration;
    private final ScreenSource screenSource;
    private final RemoteInputController inputController;
    private final ConnectionApprover approver;
    private final HostEventListener listener;
    private final AuditLog auditLog;
    private final Clock clock;
    private final SecureRandom random;
    private final ScreenEncoder encoder;
    private final AttemptLimiter attemptLimiter;
    private final PendingHandshakeLimiter pendingHandshakeLimiter;
    private final PairingTokenState pairingTokens;
    private final ChannelFactory channelFactory;
    private final ControlCommitHook controlCommitHook;
    private final Duration preAuthDeadline;
    private final ExecutorService acceptExecutor;
    private final ExecutorService sessionExecutor;
    private final ScheduledExecutorService frameExecutor;
    private final ScheduledExecutorService handshakeDeadlineExecutor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<ActiveConnection> activeConnection = new AtomicReference<>();
    private final Object connectionLifecycleLock = new Object();
    private final ConcurrentMap<SSLSocket, PendingCandidate> candidateSockets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> preAuthNotices = new ConcurrentHashMap<>();

    private volatile SSLServerSocket serverSocket;
    private volatile EphemeralTlsIdentity identity;
    private volatile PairingLink pairingLink;
    private volatile ScheduledFuture<?> pairingRotationTask;

    public HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog) {
        this(
                configuration,
                screenSource,
                inputController,
                approver,
                listener,
                auditLog,
                Clock.systemUTC(),
                new SecureRandom());
    }

    HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog,
            Clock clock,
            SecureRandom random) {
        this(
                configuration,
                screenSource,
                inputController,
                approver,
                listener,
                auditLog,
                clock,
                random,
                MessageSessionChannel::new,
                ignored -> {},
                PRE_AUTH_DEADLINE);
    }

    HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog,
            Clock clock,
            SecureRandom random,
            ControlCommitHook controlCommitHook) {
        this(
                configuration,
                screenSource,
                inputController,
                approver,
                listener,
                auditLog,
                clock,
                random,
                MessageSessionChannel::new,
                controlCommitHook,
                PRE_AUTH_DEADLINE);
    }

    HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog,
            Clock clock,
            SecureRandom random,
            ChannelFactory channelFactory,
            ControlCommitHook controlCommitHook) {
        this(
                configuration,
                screenSource,
                inputController,
                approver,
                listener,
                auditLog,
                clock,
                random,
                channelFactory,
                controlCommitHook,
                PRE_AUTH_DEADLINE);
    }

    HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog,
            Clock clock,
            SecureRandom random,
            Duration preAuthDeadline) {
        this(
                configuration,
                screenSource,
                inputController,
                approver,
                listener,
                auditLog,
                clock,
                random,
                MessageSessionChannel::new,
                ignored -> {},
                preAuthDeadline);
    }

    private HostSessionServer(
            HostConfiguration configuration,
            ScreenSource screenSource,
            RemoteInputController inputController,
            ConnectionApprover approver,
            HostEventListener listener,
            AuditLog auditLog,
            Clock clock,
            SecureRandom random,
            ChannelFactory channelFactory,
            ControlCommitHook controlCommitHook,
            Duration preAuthDeadline) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.screenSource = Objects.requireNonNull(screenSource, "screenSource");
        this.inputController = Objects.requireNonNull(inputController, "inputController");
        this.approver = Objects.requireNonNull(approver, "approver");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
        this.channelFactory = Objects.requireNonNull(channelFactory, "channelFactory");
        this.controlCommitHook = Objects.requireNonNull(controlCommitHook, "controlCommitHook");
        this.preAuthDeadline = Objects.requireNonNull(preAuthDeadline, "preAuthDeadline");
        if (preAuthDeadline.isZero() || preAuthDeadline.isNegative()) {
            throw new IllegalArgumentException("preAuthDeadline must be positive");
        }
        encoder = new ScreenEncoder(
                configuration.maximumFrameWidth(), configuration.maximumFrameHeight(), configuration.jpegQuality());
        attemptLimiter = new AttemptLimiter(MAXIMUM_FAILED_ATTEMPTS, ATTEMPT_WINDOW, clock);
        pendingHandshakeLimiter = new PendingHandshakeLimiter(MAXIMUM_PENDING_HANDSHAKES_PER_ADDRESS);
        pairingTokens = new PairingTokenState(clock, random, configuration.pairingLifetime());
        acceptExecutor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().daemon().name("jdoor-accept").factory());
        ThreadPoolExecutor workers = new ThreadPoolExecutor(
                2,
                4,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(16),
                Thread.ofPlatform().daemon().name("jdoor-session-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
        workers.allowCoreThreadTimeOut(true);
        sessionExecutor = workers;
        frameExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("jdoor-capture").factory());
        handshakeDeadlineExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("jdoor-handshake-deadline").factory());
    }

    public synchronized PairingLink start() throws IOException {
        if (closed.get()) {
            throw new IllegalStateException("Host session is closed");
        }
        if (running.get()) {
            throw new IllegalStateException("Host session is already running");
        }

        SSLServerSocket socket = null;
        try {
            EphemeralTlsIdentity newIdentity = EphemeralTlsIdentity.create(clock, random);
            socket = (SSLServerSocket)
                    newIdentity.serverContext().getServerSocketFactory().createServerSocket();
            socket.setReuseAddress(false);
            socket.setEnabledProtocols(Arrays.stream(socket.getSupportedProtocols())
                    .filter(Set.of("TLSv1.3", "TLSv1.2")::contains)
                    .toArray(String[]::new));
            socket.bind(new InetSocketAddress(configuration.bindAddress(), configuration.port()), 16);

            identity = newIdentity;
            serverSocket = socket;
            running.set(true);
            synchronized (connectionLifecycleLock) {
                if (!rotatePairingLink()) {
                    throw new IllegalStateException("Could not initialize the pairing credential");
                }
            }
            acceptExecutor.execute(this::acceptLoop);
            recordAudit("host_started", "local", "Listening on port " + socket.getLocalPort());
            notifyActivity("Session ready. Control remains off until you enable it.");
            return pairingLink;
        } catch (IOException | RuntimeException failure) {
            rollbackFailedStart(socket);
            throw failure;
        }
    }

    public PairingLink pairingLink() {
        PairingLink current = pairingLink;
        if (current == null) {
            throw new IllegalStateException("Host session has not started");
        }
        return current;
    }

    public boolean hasViewer() {
        return activeConnection.get() != null;
    }

    int pendingCandidateCount() {
        return candidateSockets.size();
    }

    int pendingHandshakeCount(String remoteAddress) {
        return pendingHandshakeLimiter.pendingFor(remoteAddress);
    }

    public boolean isControlEnabled() {
        synchronized (connectionLifecycleLock) {
            ActiveConnection connection = activeConnection.get();
            return connection != null && connection.controlEnabled.get();
        }
    }

    public void setControlEnabled(boolean enabled) {
        ActiveConnection connection = activeConnection.get();
        if (connection == null) {
            if (!enabled) {
                releaseAllInputs();
            }
            return;
        }

        controlCommitHook.beforeCommit(connection.sessionId);
        boolean sendState;
        boolean next = enabled;
        synchronized (connectionLifecycleLock) {
            if (activeConnection.get() != connection || !connection.announced) {
                return;
            }
            boolean changed = connection.controlEnabled.getAndSet(next) != next;
            if (!next) {
                releaseAllInputs();
            }
            if (changed) {
                recordAudit(
                        next ? "control_enabled" : "control_disabled", connection.remoteAddress, "Changed by the host");
                notifyControlChanged(next);
            }
            sendState = connection.channel.isOpen();
        }

        if (sendState) {
            try {
                connection.channel.send(new WireMessage.ControlState(next));
            } catch (IOException failure) {
                closeConnection(connection, "Could not update control permission");
            }
        }
    }

    public void disconnectViewer() {
        ActiveConnection connection = activeConnection.get();
        if (connection != null) {
            closeConnection(connection, "Disconnected by the host");
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                SSLServerSocket listeningSocket = serverSocket;
                if (listeningSocket == null) {
                    return;
                }
                SSLSocket socket = (SSLSocket) listeningSocket.accept();
                admitCandidate(socket);
            } catch (SocketException stopped) {
                if (running.get()) {
                    notifyError("The listening socket stopped unexpectedly", stopped);
                }
            } catch (IOException failure) {
                if (running.get()) {
                    notifyError("Could not accept a viewer connection", failure);
                }
            }
        }
    }

    private void admitCandidate(SSLSocket socket) {
        String remoteAddress = socket.getInetAddress().getHostAddress();
        if (!running.get()) {
            closeSocket(socket);
            return;
        }
        if (attemptLimiter.isBlocked(remoteAddress)) {
            closeSocket(socket);
            recordPreAuthNotice("connection_rate_limited", remoteAddress, "Pairing blocked before TLS");
            return;
        }

        PendingHandshakeLimiter.Permit permit = pendingHandshakeLimiter.tryAcquire(remoteAddress);
        if (permit == null) {
            closeSocket(socket);
            recordPreAuthNotice("pending_handshake_limited", remoteAddress, "Too many pending TLS handshakes");
            return;
        }

        PendingCandidate candidate = new PendingCandidate(permit);
        candidateSockets.put(socket, candidate);
        try {
            ScheduledFuture<?> deadlineTask = handshakeDeadlineExecutor.schedule(
                    () -> expireCandidate(socket, candidate, remoteAddress),
                    preAuthDeadline.toMillis(),
                    TimeUnit.MILLISECONDS);
            candidate.setDeadlineTask(deadlineTask);
        } catch (RejectedExecutionException stopped) {
            releaseCandidate(socket, candidate);
            return;
        }
        if (!running.get()) {
            releaseCandidate(socket, candidate);
            return;
        }
        try {
            sessionExecutor.execute(() -> {
                try {
                    handleCandidate(socket, candidate);
                } finally {
                    releaseCandidate(socket, candidate);
                }
            });
        } catch (RejectedExecutionException overloaded) {
            releaseCandidate(socket, candidate);
            recordPreAuthNotice("connection_overload", remoteAddress, "Handshake queue is full");
        }
    }

    private void handleCandidate(SSLSocket socket, PendingCandidate pendingCandidate) {
        String remoteAddress = socket.getInetAddress().getHostAddress();
        boolean authenticated = false;
        boolean sessionAttached = false;
        try (socket) {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(TLS_HANDSHAKE_TIMEOUT_MILLIS);
            TlsSocketPolicy.apply(socket);
            socket.startHandshake();
            if (!running.get()) {
                return;
            }

            socket.setSoTimeout(CLIENT_HELLO_TIMEOUT_MILLIS);
            try (SessionChannel channel = channelFactory.open(socket)) {
                if (!running.get()) {
                    return;
                }
                if (attemptLimiter.isBlocked(remoteAddress)) {
                    reject(channel, "Too many failed attempts. Try again later.");
                    recordPreAuthNotice("connection_rate_limited", remoteAddress, "Pairing blocked");
                    return;
                }
                if (activeConnection.get() != null) {
                    reject(channel, "The host is already assisting another viewer.");
                    return;
                }

                WireMessage first;
                try {
                    first = channel.read();
                } catch (ProtocolException malformedHello) {
                    attemptLimiter.registerFailure(remoteAddress);
                    throw malformedHello;
                }
                if (!(first instanceof WireMessage.ClientHello hello)) {
                    attemptLimiter.registerFailure(remoteAddress);
                    throw new ProtocolException("The first message must be a client hello");
                }

                PairingTokenState.Reservation reservation = pairingTokens.reserve(hello.token());
                if (reservation == null) {
                    attemptLimiter.registerFailure(remoteAddress);
                    reject(channel, "The pairing link is invalid or expired.");
                    recordPreAuthNotice("authentication_failed", remoteAddress, "Invalid pairing token");
                    return;
                }

                authenticated = true;
                pendingCandidate.completePreAuthentication();
                boolean reservationConsumed = false;
                try {
                    if (!running.get()) {
                        return;
                    }
                    boolean approved = approver.approve(new ConnectionRequest(
                            hello.displayName(),
                            socket.getInetAddress(),
                            identity.fingerprint().grouped().toUpperCase(Locale.ROOT)));
                    if (!approved) {
                        reject(channel, "The host declined the connection.");
                        recordAudit("consent_rejected", remoteAddress, "Request from " + hello.displayName());
                        return;
                    }
                    if (!running.get()) {
                        return;
                    }

                    socket.setSoTimeout(SESSION_READ_TIMEOUT_MILLIS);
                    ActiveConnection connection =
                            new ActiveConnection(UUID.randomUUID(), channel, hello.displayName(), remoteAddress);
                    String admissionRejection = null;
                    synchronized (connectionLifecycleLock) {
                        if (!running.get() || !activeConnection.compareAndSet(null, connection)) {
                            admissionRejection = "The host is already assisting another viewer.";
                        } else if (!pairingTokens.consume(reservation)) {
                            activeConnection.compareAndSet(connection, null);
                            admissionRejection = "The pairing link changed before the connection was admitted.";
                        } else {
                            reservationConsumed = true;
                            sessionAttached = true;
                        }
                    }
                    if (admissionRejection != null) {
                        reject(channel, admissionRejection);
                        return;
                    }

                    attemptLimiter.registerSuccess(remoteAddress);
                    runAcceptedSession(connection);
                } finally {
                    if (!reservationConsumed && pairingTokens.release(reservation)) {
                        rotateReleasedPairingCredential();
                    }
                }
            }
        } catch (EOFException disconnected) {
            if (running.get()) {
                recordPreAuthNotice("pre_auth_connection_closed", remoteAddress, "Connection closed during setup");
            }
        } catch (Exception failure) {
            if (!running.get()) {
                return;
            }
            if (authenticated || sessionAttached) {
                recordAudit("connection_error", remoteAddress, safeMessage(failure));
                notifyError("A viewer connection failed", failure);
            } else {
                recordPreAuthNotice("pre_auth_connection_error", remoteAddress, safeMessage(failure));
            }
        }
    }

    private void runAcceptedSession(ActiveConnection connection) throws IOException {
        String disconnectReason = "Viewer disconnected";
        try {
            Rectangle bounds = screenSource.bounds();
            connection.channel.send(
                    new WireMessage.ServerHello(connection.sessionId, bounds.width, bounds.height, false));
            synchronized (connectionLifecycleLock) {
                if (!running.get() || activeConnection.get() != connection) {
                    return;
                }
                connection.announced = true;
                startFrameStream(connection);
                recordAudit("viewer_connected", connection.remoteAddress, "Approved " + connection.displayName);
                notifyViewerConnected(connection.displayName, connection.remoteAddress);
            }

            while (running.get() && activeConnection.get() == connection && connection.channel.isOpen()) {
                WireMessage message = connection.channel.read();
                if (message instanceof WireMessage.PointerInput pointer) {
                    applyPointerIfPermitted(connection, pointer);
                } else if (message instanceof WireMessage.KeyboardInput keyboard) {
                    applyKeyboardIfPermitted(connection, keyboard);
                } else if (message instanceof WireMessage.ReleaseAllInputs) {
                    releaseAllInputs();
                } else if (message instanceof WireMessage.Ping ping) {
                    connection.channel.send(new WireMessage.Pong(ping.nonce()));
                } else if (message instanceof WireMessage.Goodbye goodbye) {
                    disconnectReason = goodbye.reason();
                    break;
                } else {
                    throw new ProtocolException("Viewer sent a message that is not allowed in this direction");
                }
            }
        } catch (EOFException | SocketException disconnected) {
            disconnectReason = "Connection closed";
        } finally {
            finishConnection(connection, disconnectReason);
        }
    }

    private void applyPointerIfPermitted(ActiveConnection connection, WireMessage.PointerInput pointer) {
        synchronized (connectionLifecycleLock) {
            if (activeConnection.get() == connection && connection.controlEnabled.get()) {
                inputController.apply(pointer);
            }
        }
    }

    private void applyKeyboardIfPermitted(ActiveConnection connection, WireMessage.KeyboardInput keyboard) {
        synchronized (connectionLifecycleLock) {
            if (activeConnection.get() == connection && connection.controlEnabled.get()) {
                inputController.apply(keyboard);
            }
        }
    }

    private void startFrameStream(ActiveConnection connection) {
        connection.frameSequence.set(0);
        long intervalMillis = Math.max(50L, 1_000L / configuration.framesPerSecond());
        ScheduledFuture<?> task = frameExecutor.scheduleWithFixedDelay(
                () -> sendFrame(connection), intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        connection.frameTask.set(task);
    }

    private void sendFrame(ActiveConnection connection) {
        if (!running.get() || activeConnection.get() != connection) {
            return;
        }
        try {
            ScreenEncoder.EncodedScreen screen = encoder.encode(screenSource.capture());
            connection.channel.send(new WireMessage.ScreenFrame(
                    connection.frameSequence.getAndIncrement(),
                    clock.millis(),
                    screen.width(),
                    screen.height(),
                    screen.jpeg()));
        } catch (Exception failure) {
            if (activeConnection.get() == connection) {
                notifyError("A screen frame could not be sent", failure);
                closeConnection(connection, "Screen sharing failed");
            }
        }
    }

    private void finishConnection(ActiveConnection connection, String reason) {
        synchronized (connectionLifecycleLock) {
            if (!activeConnection.compareAndSet(connection, null)) {
                return;
            }
            connection.controlEnabled.set(false);
            ScheduledFuture<?> frameTask = connection.frameTask.getAndSet(null);
            if (frameTask != null) {
                frameTask.cancel(true);
            }
            releaseAllInputs();
            closeChannel(connection.channel);

            if (connection.announced) {
                recordAudit("viewer_disconnected", connection.remoteAddress, reason);
                notifyControlChanged(false);
                notifyViewerDisconnected(reason);
            }
            if (running.get()) {
                rotatePairingLink();
            }
        }
    }

    private void closeConnection(ActiveConnection connection, String reason) {
        finishConnection(connection, reason);
    }

    private void reject(SessionChannel channel, String reason) throws IOException {
        channel.send(new WireMessage.Rejected(reason));
    }

    private void rotateReleasedPairingCredential() {
        synchronized (connectionLifecycleLock) {
            if (running.get() && activeConnection.get() == null && rotatePairingLink()) {
                notifyActivity("The expired pairing link was rotated.");
            }
        }
    }

    private boolean rotatePairingLink() {
        PairingTokenState.Credential credential = pairingTokens.rotate();
        if (credential == null) {
            return false;
        }
        String advertisedHost = configuration.advertisedHost() == null
                ? NetworkAddresses.preferredLanAddress()
                : configuration.advertisedHost();
        PairingLink next = new PairingLink(
                advertisedHost, serverSocket.getLocalPort(), credential.token(), identity.fingerprint());
        pairingLink = next;
        notifyPairingLinkChanged(next);

        ScheduledFuture<?> previousRotation = pairingRotationTask;
        pairingRotationTask = null;
        if (previousRotation != null) {
            previousRotation.cancel(false);
        }
        if (running.get() && activeConnection.get() == null) {
            pairingRotationTask = frameExecutor.schedule(
                    () -> {
                        synchronized (connectionLifecycleLock) {
                            if (running.get() && activeConnection.get() == null && rotatePairingLink()) {
                                notifyActivity("The expired pairing link was rotated.");
                            }
                        }
                    },
                    configuration.pairingLifetime().toMillis(),
                    TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void rollbackFailedStart(SSLServerSocket socket) {
        running.set(false);
        closeSocket(socket);
        ScheduledFuture<?> rotation = pairingRotationTask;
        pairingRotationTask = null;
        if (rotation != null) {
            rotation.cancel(true);
        }
        serverSocket = null;
        identity = null;
        pairingLink = null;
    }

    private void expireCandidate(SSLSocket socket, PendingCandidate candidate, String remoteAddress) {
        if (candidate.expirePreAuthentication() && candidateSockets.remove(socket, candidate)) {
            closeSocket(socket);
            recordPreAuthNotice(
                    "pre_auth_deadline_exceeded",
                    remoteAddress,
                    "TLS handshake and client hello exceeded the absolute deadline");
        }
    }

    private void releaseCandidate(SSLSocket socket, PendingCandidate candidate) {
        candidateSockets.remove(socket, candidate);
        candidate.completePreAuthentication();
        closeSocket(socket);
    }

    private void closeCandidates() {
        for (Map.Entry<SSLSocket, PendingCandidate> candidate : candidateSockets.entrySet()) {
            if (candidateSockets.remove(candidate.getKey(), candidate.getValue())) {
                candidate.getValue().completePreAuthentication();
                closeSocket(candidate.getKey());
            }
        }
    }

    private void recordPreAuthNotice(String type, String remoteAddress, String detail) {
        Instant now = clock.instant();
        String key = type + '\u0000' + remoteAddress;
        AtomicBoolean shouldRecord = new AtomicBoolean();
        preAuthNotices.compute(key, (ignored, previous) -> {
            if (previous == null || !now.isBefore(previous.plus(PRE_AUTH_NOTICE_INTERVAL))) {
                shouldRecord.set(true);
                return now;
            }
            return previous;
        });
        if (shouldRecord.get()) {
            recordAudit(type, remoteAddress, detail);
        }
        if (preAuthNotices.size() > 1_024) {
            Instant cutoff = now.minus(PRE_AUTH_NOTICE_INTERVAL.multipliedBy(2));
            preAuthNotices.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        }
    }

    private void releaseAllInputs() {
        try {
            inputController.releaseAll();
        } catch (RuntimeException failure) {
            notifyError("Remote input state could not be released", failure);
        }
    }

    private void recordAudit(String type, String remoteAddress, String detail) {
        try {
            auditLog.record(new AuditEvent(clock.instant(), type, remoteAddress, detail));
        } catch (IOException failure) {
            notifyError("The local audit log could not be updated", failure);
        }
    }

    private void notifyPairingLinkChanged(PairingLink link) {
        safely(() -> listener.onPairingLinkChanged(link));
    }

    private void notifyViewerConnected(String displayName, String remoteAddress) {
        safely(() -> listener.onViewerConnected(displayName, remoteAddress));
    }

    private void notifyViewerDisconnected(String reason) {
        safely(() -> listener.onViewerDisconnected(reason));
    }

    private void notifyControlChanged(boolean enabled) {
        safely(() -> listener.onControlChanged(enabled));
    }

    private void notifyActivity(String message) {
        safely(() -> listener.onActivity(message));
    }

    private void notifyError(String message, Throwable cause) {
        safely(() -> listener.onError(message, cause));
    }

    private static void safely(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException ignored) {
            // A presentation-layer callback must not stop a network session.
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 480));
    }

    private static void closeSocket(Closeable socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Socket closure is best-effort.
        }
    }

    private static void closeChannel(SessionChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
            // Socket closure is authoritative even when the protocol cannot finish cleanly.
        }
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        boolean wasRunning = running.getAndSet(false);
        SSLServerSocket listeningSocket = serverSocket;
        serverSocket = null;
        closeSocket(listeningSocket);

        ActiveConnection connection = activeConnection.get();
        if (connection != null) {
            closeConnection(connection, "Host stopped sharing");
        }
        closeCandidates();

        ScheduledFuture<?> rotation = pairingRotationTask;
        pairingRotationTask = null;
        if (rotation != null) {
            rotation.cancel(true);
        }
        releaseAllInputs();
        frameExecutor.shutdownNow();
        handshakeDeadlineExecutor.shutdownNow();
        sessionExecutor.shutdownNow();
        acceptExecutor.shutdownNow();
        if (wasRunning) {
            recordAudit("host_stopped", "local", "Session stopped");
            notifyActivity("Session stopped.");
        }
    }

    @FunctionalInterface
    interface ChannelFactory {
        SessionChannel open(SSLSocket socket) throws IOException;
    }

    @FunctionalInterface
    interface ControlCommitHook {
        void beforeCommit(UUID sessionId);
    }

    interface SessionChannel extends AutoCloseable {
        WireMessage read() throws IOException;

        void send(WireMessage message) throws IOException;

        boolean isOpen();

        @Override
        void close() throws IOException;
    }

    private static final class MessageSessionChannel implements SessionChannel {
        private final MessageChannel delegate;

        private MessageSessionChannel(SSLSocket socket) throws IOException {
            delegate = new MessageChannel(socket);
        }

        @Override
        public WireMessage read() throws IOException {
            return delegate.read();
        }

        @Override
        public void send(WireMessage message) throws IOException {
            delegate.send(message);
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static final class ActiveConnection {
        private final UUID sessionId;
        private final SessionChannel channel;
        private final String displayName;
        private final String remoteAddress;
        private final AtomicBoolean controlEnabled = new AtomicBoolean();
        private final AtomicLong frameSequence = new AtomicLong();
        private final AtomicReference<ScheduledFuture<?>> frameTask = new AtomicReference<>();
        private boolean announced;

        private ActiveConnection(UUID sessionId, SessionChannel channel, String displayName, String remoteAddress) {
            this.sessionId = sessionId;
            this.channel = channel;
            this.displayName = displayName;
            this.remoteAddress = remoteAddress;
        }
    }

    private static final class PendingCandidate {
        private final PendingHandshakeLimiter.Permit permit;
        private final AtomicBoolean preAuthenticationComplete = new AtomicBoolean();
        private final AtomicReference<ScheduledFuture<?>> deadlineTask = new AtomicReference<>();

        private PendingCandidate(PendingHandshakeLimiter.Permit permit) {
            this.permit = permit;
        }

        private void setDeadlineTask(ScheduledFuture<?> task) {
            if (!deadlineTask.compareAndSet(null, task) || preAuthenticationComplete.get()) {
                task.cancel(false);
            }
        }

        private boolean expirePreAuthentication() {
            if (!preAuthenticationComplete.compareAndSet(false, true)) {
                return false;
            }
            permit.close();
            return true;
        }

        private void completePreAuthentication() {
            if (preAuthenticationComplete.compareAndSet(false, true)) {
                permit.close();
            }
            ScheduledFuture<?> task = deadlineTask.get();
            if (task != null) {
                task.cancel(false);
            }
        }
    }
}
