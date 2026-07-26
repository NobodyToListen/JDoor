package com.jdoor.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public final class MessageCodec {
    static final int MAGIC = 0x4A_44_4F_52;
    static final int VERSION = 1;
    public static final int MAX_IMAGE_BYTES = 6 * 1024 * 1024;
    public static final int MAX_PAYLOAD_BYTES = MAX_IMAGE_BYTES + 64 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;

    public void write(DataOutputStream output, WireMessage message) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream payload = new DataOutputStream(buffer)) {
            MessageType type = encodePayload(payload, message);
            payload.flush();
            byte[] bytes = buffer.toByteArray();
            if (bytes.length > MAX_PAYLOAD_BYTES) {
                throw new ProtocolException("Encoded payload exceeds the protocol limit");
            }
            output.writeInt(MAGIC);
            output.writeShort(VERSION);
            output.writeByte(type.code);
            output.writeInt(bytes.length);
            output.write(bytes);
            output.flush();
        }
    }

    public WireMessage read(DataInputStream input) throws IOException {
        int magic;
        try {
            magic = input.readInt();
        } catch (EOFException disconnected) {
            throw disconnected;
        }
        if (magic != MAGIC) {
            throw new ProtocolException("Invalid protocol magic");
        }
        int version = input.readUnsignedShort();
        if (version != VERSION) {
            throw new ProtocolException("Unsupported protocol version: " + version);
        }
        MessageType type = MessageType.fromCode(input.readUnsignedByte());
        int length = input.readInt();
        if (length < 0 || length > MAX_PAYLOAD_BYTES) {
            throw new ProtocolException("Payload length is outside the allowed range");
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException("Connection closed inside a protocol frame");
        }
        try (DataInputStream payload = new DataInputStream(new ByteArrayInputStream(bytes))) {
            WireMessage message = decodePayload(payload, type);
            if (payload.available() != 0) {
                throw new ProtocolException("Protocol frame contains trailing data");
            }
            return message;
        } catch (IllegalArgumentException invalidPayload) {
            throw new ProtocolException("Protocol frame contains invalid data", invalidPayload);
        }
    }

    private static MessageType encodePayload(DataOutputStream output, WireMessage message) throws IOException {
        return switch (message) {
            case WireMessage.ClientHello hello -> {
                writeString(output, hello.token());
                writeString(output, hello.displayName());
                yield MessageType.CLIENT_HELLO;
            }
            case WireMessage.ServerHello hello -> {
                output.writeLong(hello.sessionId().getMostSignificantBits());
                output.writeLong(hello.sessionId().getLeastSignificantBits());
                output.writeInt(hello.screenWidth());
                output.writeInt(hello.screenHeight());
                output.writeBoolean(hello.controlEnabled());
                yield MessageType.SERVER_HELLO;
            }
            case WireMessage.Rejected rejected -> {
                writeString(output, rejected.reason());
                yield MessageType.REJECTED;
            }
            case WireMessage.ScreenFrame frame -> {
                byte[] jpeg = frame.jpeg();
                output.writeLong(frame.sequence());
                output.writeLong(frame.capturedAtEpochMillis());
                output.writeInt(frame.width());
                output.writeInt(frame.height());
                output.writeInt(jpeg.length);
                output.write(jpeg);
                yield MessageType.SCREEN_FRAME;
            }
            case WireMessage.PointerInput pointer -> {
                output.writeByte(pointer.action().ordinal());
                output.writeFloat(pointer.normalizedX());
                output.writeFloat(pointer.normalizedY());
                output.writeByte(pointer.button());
                yield MessageType.POINTER_INPUT;
            }
            case WireMessage.KeyboardInput keyboard -> {
                output.writeByte(keyboard.action().ordinal());
                output.writeInt(keyboard.keyCode());
                output.writeInt(keyboard.modifiers());
                yield MessageType.KEYBOARD_INPUT;
            }
            case WireMessage.ReleaseAllInputs ignored -> MessageType.RELEASE_ALL_INPUTS;
            case WireMessage.ControlState state -> {
                output.writeBoolean(state.enabled());
                yield MessageType.CONTROL_STATE;
            }
            case WireMessage.Ping ping -> {
                output.writeLong(ping.nonce());
                yield MessageType.PING;
            }
            case WireMessage.Pong pong -> {
                output.writeLong(pong.nonce());
                yield MessageType.PONG;
            }
            case WireMessage.Goodbye goodbye -> {
                writeString(output, goodbye.reason());
                yield MessageType.GOODBYE;
            }
        };
    }

    private static WireMessage decodePayload(DataInputStream input, MessageType type) throws IOException {
        return switch (type) {
            case CLIENT_HELLO -> new WireMessage.ClientHello(readString(input), readString(input));
            case SERVER_HELLO ->
                new WireMessage.ServerHello(
                        new UUID(input.readLong(), input.readLong()),
                        input.readInt(),
                        input.readInt(),
                        input.readBoolean());
            case REJECTED -> new WireMessage.Rejected(readString(input));
            case SCREEN_FRAME -> {
                long sequence = input.readLong();
                long capturedAt = input.readLong();
                int width = input.readInt();
                int height = input.readInt();
                int imageLength = input.readInt();
                if (imageLength < 1 || imageLength > MAX_IMAGE_BYTES || imageLength > input.available()) {
                    throw new ProtocolException("JPEG length is outside the allowed range");
                }
                byte[] jpeg = input.readNBytes(imageLength);
                yield new WireMessage.ScreenFrame(sequence, capturedAt, width, height, jpeg);
            }
            case POINTER_INPUT ->
                new WireMessage.PointerInput(
                        enumValue(WireMessage.PointerAction.values(), input.readUnsignedByte(), "pointer action"),
                        input.readFloat(),
                        input.readFloat(),
                        input.readUnsignedByte());
            case KEYBOARD_INPUT ->
                new WireMessage.KeyboardInput(
                        enumValue(WireMessage.KeyAction.values(), input.readUnsignedByte(), "key action"),
                        input.readInt(),
                        input.readInt());
            case RELEASE_ALL_INPUTS -> new WireMessage.ReleaseAllInputs();
            case CONTROL_STATE -> new WireMessage.ControlState(input.readBoolean());
            case PING -> new WireMessage.Ping(input.readLong());
            case PONG -> new WireMessage.Pong(input.readLong());
            case GOODBYE -> new WireMessage.Goodbye(readString(input));
        };
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new ProtocolException("String value exceeds the protocol limit");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new ProtocolException("String length is outside the allowed range");
        }
        byte[] bytes = input.readNBytes(length);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (!Arrays.equals(bytes, value.getBytes(StandardCharsets.UTF_8))) {
            throw new ProtocolException("String is not valid UTF-8");
        }
        return value;
    }

    private static <T> T enumValue(T[] values, int ordinal, String field) throws ProtocolException {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new ProtocolException("Unknown " + field + ": " + ordinal);
        }
        return values[ordinal];
    }

    private enum MessageType {
        CLIENT_HELLO(1),
        SERVER_HELLO(2),
        REJECTED(3),
        SCREEN_FRAME(4),
        POINTER_INPUT(5),
        KEYBOARD_INPUT(6),
        CONTROL_STATE(7),
        PING(8),
        PONG(9),
        GOODBYE(10),
        RELEASE_ALL_INPUTS(11);

        private final int code;

        MessageType(int code) {
            this.code = code;
        }

        private static MessageType fromCode(int code) throws ProtocolException {
            return Arrays.stream(values())
                    .filter(value -> value.code == code)
                    .findFirst()
                    .orElseThrow(() -> new ProtocolException("Unknown message type: " + code));
        }
    }
}
