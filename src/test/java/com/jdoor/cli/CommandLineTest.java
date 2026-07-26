package com.jdoor.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jdoor.AppInfo;
import org.junit.jupiter.api.Test;

class CommandLineTest {
    @Test
    void defaultsToLauncher() {
        CommandLine options = CommandLine.parse(new String[0]);

        assertEquals(CommandLine.Mode.LAUNCHER, options.mode());
        assertEquals(AppInfo.DEFAULT_PORT, options.port());
        assertEquals(null, options.advertisedHost());
    }

    @Test
    void parsesHostPort() {
        CommandLine options =
                CommandLine.parse(new String[] {"--port", "9443", "--advertise", "192.168.10.8", "--host"});

        assertEquals(CommandLine.Mode.HOST, options.mode());
        assertEquals(9443, options.port());
        assertEquals("192.168.10.8", options.advertisedHost());
    }

    @Test
    void parsesPairingLinkWithoutInterpretingIt() {
        CommandLine options = CommandLine.parse(new String[] {"--join", "jdoor://127.0.0.1:8443/join?x=y"});

        assertEquals(CommandLine.Mode.JOIN, options.mode());
        assertEquals("jdoor://127.0.0.1:8443/join?x=y", options.pairingLink());
    }

    @Test
    void rejectsConflictingModesAndInvalidPorts() {
        assertThrows(
                IllegalArgumentException.class, () -> CommandLine.parse(new String[] {"--host", "--join", "link"}));
        assertThrows(
                IllegalArgumentException.class, () -> CommandLine.parse(new String[] {"--host", "--port", "70000"}));
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse(new String[] {"--join"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> CommandLine.parse(new String[] {"--join", "link", "--advertise", "host"}));
    }

    @Test
    void helpDocumentsSafetyDefaults() {
        String help = CommandLine.help();

        assertTrue(help.contains("local approval"));
        assertTrue(help.contains("disabled until the host explicitly enables it"));
        assertTrue(help.contains("--join"));
    }
}
