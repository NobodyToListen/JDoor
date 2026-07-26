package com.jdoor.cli;

import com.jdoor.AppInfo;
import java.util.Objects;

public record CommandLine(Mode mode, int port, String pairingLink, String advertisedHost) {
    public CommandLine {
        Objects.requireNonNull(mode, "mode");
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (mode == Mode.JOIN && (pairingLink == null || pairingLink.isBlank())) {
            throw new IllegalArgumentException("--join requires a pairing link");
        }
        if (mode != Mode.JOIN && pairingLink != null) {
            throw new IllegalArgumentException("A pairing link is only valid with --join");
        }
        if (advertisedHost != null) {
            advertisedHost = advertisedHost.strip();
            if (mode != Mode.HOST) {
                throw new IllegalArgumentException("--advertise is only valid with --host");
            }
            if (advertisedHost.isEmpty()
                    || advertisedHost.length() > 253
                    || advertisedHost.contains("/")
                    || advertisedHost.contains("@")) {
                throw new IllegalArgumentException("--advertise requires a valid host or IP address");
            }
        }
    }

    public static CommandLine parse(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        Mode mode = Mode.LAUNCHER;
        int port = AppInfo.DEFAULT_PORT;
        String pairingLink = null;
        String advertisedHost = null;

        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            switch (argument) {
                case "--help", "-h" -> mode = exclusive(mode, Mode.HELP, argument);
                case "--version", "-v" -> mode = exclusive(mode, Mode.VERSION, argument);
                case "--host" -> mode = exclusive(mode, Mode.HOST, argument);
                case "--join" -> {
                    mode = exclusive(mode, Mode.JOIN, argument);
                    if (++index >= arguments.length) {
                        throw new IllegalArgumentException("--join requires a pairing link");
                    }
                    pairingLink = arguments[index];
                }
                case "--port" -> {
                    if (++index >= arguments.length) {
                        throw new IllegalArgumentException("--port requires a number");
                    }
                    try {
                        port = Integer.parseInt(arguments[index]);
                    } catch (NumberFormatException invalidPort) {
                        throw new IllegalArgumentException("--port must be a number", invalidPort);
                    }
                }
                case "--advertise" -> {
                    if (++index >= arguments.length) {
                        throw new IllegalArgumentException("--advertise requires a host or IP address");
                    }
                    advertisedHost = arguments[index];
                }
                default -> throw new IllegalArgumentException("Unknown option: " + argument);
            }
        }
        return new CommandLine(mode, port, pairingLink, advertisedHost);
    }

    public static String help() {
        return """
                JDoor Assist - consent-first remote assistance

                Usage:
                  java -jar jdoor-assist.jar                 Open the launcher
                  java -jar jdoor-assist.jar --host          Start a host session
                  java -jar jdoor-assist.jar --join <link>   Join with a pairing link

                Options:
                  --port <1-65535>  Host port (default: 8443)
                  --advertise <host> Override the LAN address placed in host links
                  --help, -h        Show this help
                  --version, -v     Show the version

                JDoor Assist is designed for authorized support on trusted local networks.
                Screen sharing is visible, every viewer requires local approval, and remote
                control is disabled until the host explicitly enables it.
                """;
    }

    private static Mode exclusive(Mode current, Mode next, String argument) {
        if (current != Mode.LAUNCHER) {
            throw new IllegalArgumentException("Conflicting mode option " + argument + " after "
                    + current.name().toLowerCase());
        }
        return next;
    }

    public enum Mode {
        LAUNCHER,
        HOST,
        JOIN,
        HELP,
        VERSION
    }
}
