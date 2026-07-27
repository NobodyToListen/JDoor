package com.jdoor.protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MessageChannel implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final MessageCodec codec;
    private final AtomicBoolean closed = new AtomicBoolean();

    public MessageChannel(Socket socket) throws IOException {
        this(socket, new MessageCodec());
    }

    MessageChannel(Socket socket, MessageCodec codec) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.codec = Objects.requireNonNull(codec, "codec");
        input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public WireMessage read() throws IOException {
        if (closed.get()) {
            throw new IOException("Channel is closed");
        }
        return codec.read(input);
    }

    public synchronized void send(WireMessage message) throws IOException {
        if (closed.get()) {
            throw new IOException("Channel is closed");
        }
        codec.write(output, Objects.requireNonNull(message, "message"));
    }

    public boolean isOpen() {
        return !closed.get() && !socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            socket.close();
        }
    }
}
