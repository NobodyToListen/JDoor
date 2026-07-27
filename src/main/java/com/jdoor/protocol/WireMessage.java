package com.jdoor.protocol;

import com.jdoor.security.SessionToken;
import java.util.Objects;
import java.util.UUID;

public sealed interface WireMessage
        permits WireMessage.ClientHello,
                WireMessage.ServerHello,
                WireMessage.Rejected,
                WireMessage.ScreenFrame,
                WireMessage.PointerInput,
                WireMessage.KeyboardInput,
                WireMessage.ReleaseAllInputs,
                WireMessage.ControlState,
                WireMessage.Ping,
                WireMessage.Pong,
                WireMessage.Goodbye {

    record ClientHello(String token, String displayName) implements WireMessage {
        public ClientHello {
            new SessionToken(token);
            displayName = boundedText(displayName, "displayName", 1, 80);
        }
    }

    record ServerHello(UUID sessionId, int screenWidth, int screenHeight, boolean controlEnabled)
            implements WireMessage {
        public ServerHello {
            Objects.requireNonNull(sessionId, "sessionId");
            FrameLimits.requireSafeDimensions(screenWidth, screenHeight);
        }
    }

    record Rejected(String reason) implements WireMessage {
        public Rejected {
            reason = boundedText(reason, "reason", 1, 512);
        }
    }

    record ScreenFrame(long sequence, long capturedAtEpochMillis, int width, int height, byte[] jpeg)
            implements WireMessage {
        public ScreenFrame {
            if (sequence < 0) {
                throw new IllegalArgumentException("sequence cannot be negative");
            }
            if (capturedAtEpochMillis < 0) {
                throw new IllegalArgumentException("capture timestamp cannot be negative");
            }
            FrameLimits.requireSafeDimensions(width, height);
            jpeg = Objects.requireNonNull(jpeg, "jpeg").clone();
            if (jpeg.length == 0 || jpeg.length > MessageCodec.MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("JPEG payload has an invalid size");
            }
        }

        @Override
        public byte[] jpeg() {
            return jpeg.clone();
        }
    }

    record PointerInput(PointerAction action, float normalizedX, float normalizedY, int button) implements WireMessage {
        public PointerInput {
            Objects.requireNonNull(action, "action");
            requireNormalized(normalizedX, "normalizedX");
            requireNormalized(normalizedY, "normalizedY");
            if (button < 0 || button > 3) {
                throw new IllegalArgumentException("button must be between 0 and 3");
            }
            if ((action == PointerAction.PRESS || action == PointerAction.RELEASE) && button == 0) {
                throw new IllegalArgumentException("press/release requires a mouse button");
            }
        }
    }

    record KeyboardInput(KeyAction action, int keyCode, int modifiers) implements WireMessage {
        public KeyboardInput {
            Objects.requireNonNull(action, "action");
            if (keyCode < 1 || keyCode > 65_535) {
                throw new IllegalArgumentException("keyCode must be between 1 and 65535");
            }
            if (modifiers < 0 || modifiers > 65_535) {
                throw new IllegalArgumentException("modifiers must be between 0 and 65535");
            }
        }
    }

    record ReleaseAllInputs() implements WireMessage {}

    record ControlState(boolean enabled) implements WireMessage {}

    record Ping(long nonce) implements WireMessage {}

    record Pong(long nonce) implements WireMessage {}

    record Goodbye(String reason) implements WireMessage {
        public Goodbye {
            reason = boundedText(reason, "reason", 1, 512);
        }
    }

    enum PointerAction {
        MOVE,
        PRESS,
        RELEASE
    }

    enum KeyAction {
        PRESS,
        RELEASE
    }

    private static String boundedText(String value, String field, int minimum, int maximum) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.length() < minimum || value.length() > maximum) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum + " characters");
        }
        if (value.chars().anyMatch(character -> Character.isISOControl(character))) {
            throw new IllegalArgumentException(field + " cannot contain control characters");
        }
        return value;
    }

    private static void requireNormalized(float value, String field) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(field + " must be a finite value from 0 to 1");
        }
    }
}
