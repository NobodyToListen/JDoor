package com.jdoor.protocol;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WireMessageValidationTest {
    @Test
    void rejectsOutOfRangeCoordinatesButtonsAndKeys() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WireMessage.PointerInput(WireMessage.PointerAction.MOVE, Float.NaN, 0.5f, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new WireMessage.PointerInput(WireMessage.PointerAction.PRESS, 0.5f, 0.5f, 0));
        assertThrows(
                IllegalArgumentException.class, () -> new WireMessage.KeyboardInput(WireMessage.KeyAction.PRESS, 0, 0));
    }

    @Test
    void rejectsInvalidDimensionsAndOversizedImages() {
        assertThrows(
                IllegalArgumentException.class, () -> new WireMessage.ServerHello(UUID.randomUUID(), 0, 1080, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new WireMessage.ScreenFrame(1, 1, 10, 10, new byte[MessageCodec.MAX_IMAGE_BYTES + 1]));
    }

    @Test
    void rejectsControlCharactersInUserFacingText() {
        assertThrows(IllegalArgumentException.class, () -> new WireMessage.Goodbye("hidden\nline"));
        assertThrows(IllegalArgumentException.class, () -> new WireMessage.Rejected(" "));
    }
}
