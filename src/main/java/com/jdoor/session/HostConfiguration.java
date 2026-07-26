package com.jdoor.session;

import com.jdoor.protocol.FrameLimits;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

public record HostConfiguration(
        InetAddress bindAddress,
        String advertisedHost,
        int port,
        int framesPerSecond,
        int maximumFrameWidth,
        int maximumFrameHeight,
        float jpegQuality,
        Duration pairingLifetime) {

    public HostConfiguration {
        if (bindAddress == null) {
            throw new IllegalArgumentException("bindAddress is required");
        }
        if (advertisedHost != null && advertisedHost.isBlank()) {
            advertisedHost = null;
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (framesPerSecond < 1 || framesPerSecond > 15) {
            throw new IllegalArgumentException("framesPerSecond must be between 1 and 15");
        }
        if (maximumFrameWidth < 320 || maximumFrameWidth > 7_680) {
            throw new IllegalArgumentException("maximumFrameWidth is outside the supported range");
        }
        if (maximumFrameHeight < 240 || maximumFrameHeight > 4_320) {
            throw new IllegalArgumentException("maximumFrameHeight is outside the supported range");
        }
        if (!FrameLimits.isSafeDimensions(maximumFrameWidth, maximumFrameHeight)) {
            throw new IllegalArgumentException("Configured frame dimensions exceed the supported safety limit");
        }
        if (!Float.isFinite(jpegQuality) || jpegQuality < 0.25f || jpegQuality > 0.95f) {
            throw new IllegalArgumentException("jpegQuality must be between 0.25 and 0.95");
        }
        if (pairingLifetime == null
                || pairingLifetime.compareTo(Duration.ofMinutes(1)) < 0
                || pairingLifetime.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("pairingLifetime must be between 1 and 60 minutes");
        }
    }

    public static HostConfiguration defaults(int port) {
        return defaults(port, null);
    }

    public static HostConfiguration defaults(int port, String advertisedHost) {
        try {
            return new HostConfiguration(
                    InetAddress.getByName("0.0.0.0"),
                    advertisedHost,
                    port,
                    4,
                    1_600,
                    900,
                    0.65f,
                    Duration.ofMinutes(10));
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException("IPv4 wildcard address is unavailable", impossible);
        }
    }
}
