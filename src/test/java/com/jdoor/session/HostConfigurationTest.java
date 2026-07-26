package com.jdoor.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HostConfigurationTest {
    @Test
    void defaultsAreBoundedAndControlSafe() {
        HostConfiguration configuration = HostConfiguration.defaults(8443);

        assertEquals(4, configuration.framesPerSecond());
        assertEquals(Duration.ofMinutes(10), configuration.pairingLifetime());
        assertFalse(configuration.jpegQuality() > 0.95f);
    }

    @Test
    void rejectsUnboundedStreamingSettings() {
        HostConfiguration base = HostConfiguration.defaults(8443);

        assertThrows(
                IllegalArgumentException.class,
                () -> new HostConfiguration(
                        base.bindAddress(), null, 8443, 60, 1600, 900, 0.65f, Duration.ofMinutes(10)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostConfiguration(
                        base.bindAddress(), null, 8443, 4, 1600, 900, 1.0f, Duration.ofMinutes(10)));
    }
}
