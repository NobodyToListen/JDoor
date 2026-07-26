package com.jdoor.protocol;

public final class FrameLimits {
    public static final int MAXIMUM_DIMENSION = 8_192;
    public static final long MAXIMUM_PIXELS = 16_777_216L;

    private FrameLimits() {}

    public static boolean isSafeDimensions(int width, int height) {
        return width > 0
                && height > 0
                && width <= MAXIMUM_DIMENSION
                && height <= MAXIMUM_DIMENSION
                && (long) width * height <= MAXIMUM_PIXELS;
    }

    static void requireSafeDimensions(int width, int height) {
        if (!isSafeDimensions(width, height)) {
            throw new IllegalArgumentException("Frame dimensions exceed the supported safety limit");
        }
    }
}
