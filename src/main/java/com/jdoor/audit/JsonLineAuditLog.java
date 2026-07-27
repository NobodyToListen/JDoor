package com.jdoor.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class JsonLineAuditLog implements AuditLog {
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZoneOffset.UTC);
    private static final Pattern OWNED_LOG_FILE = Pattern.compile("session-\\d{4}-\\d{2}-\\d{2}\\.jsonl");
    private static final long DEFAULT_MAXIMUM_FILE_BYTES = 5L * 1_024 * 1_024;
    private static final Duration DEFAULT_RETENTION = Duration.ofDays(30);

    private final Path directory;
    private final Clock clock;
    private final long maximumFileBytes;
    private final Duration retention;
    private String lastRetentionSweep;

    public JsonLineAuditLog(Path directory, Clock clock) {
        this(directory, clock, DEFAULT_MAXIMUM_FILE_BYTES, DEFAULT_RETENTION);
    }

    JsonLineAuditLog(Path directory, Clock clock, long maximumFileBytes, Duration retention) {
        this.directory =
                Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
        if (maximumFileBytes < 256) {
            throw new IllegalArgumentException("maximumFileBytes must be at least 256");
        }
        this.maximumFileBytes = maximumFileBytes;
        this.retention = Objects.requireNonNull(retention, "retention");
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("retention must be positive");
        }
    }

    @Override
    public synchronized void record(AuditEvent event) throws IOException {
        Files.createDirectories(directory);
        sweepExpiredLogs();
        Path file = directory.resolve("session-" + FILE_DATE.format(clock.instant()) + ".jsonl");
        String line = "{\"timestamp\":\""
                + escape(event.timestamp().toString())
                + "\",\"type\":\""
                + escape(event.type())
                + "\",\"remoteAddress\":\""
                + escape(event.remoteAddress())
                + "\",\"detail\":\""
                + escape(event.detail())
                + "\"}\n";
        long nextLineBytes = line.getBytes(StandardCharsets.UTF_8).length;
        long currentBytes = Files.exists(file) ? Files.size(file) : 0;
        if (currentBytes + nextLineBytes > maximumFileBytes) {
            return;
        }
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void sweepExpiredLogs() {
        String sweepDate = FILE_DATE.format(clock.instant());
        if (sweepDate.equals(lastRetentionSweep)) {
            return;
        }
        lastRetentionSweep = sweepDate;
        Instant cutoff = clock.instant().minus(retention);
        try (Stream<Path> entries = Files.list(directory)) {
            entries.filter(path -> OWNED_LOG_FILE
                            .matcher(path.getFileName().toString())
                            .matches())
                    .filter(path -> isOlderThan(path, cutoff))
                    .forEach(JsonLineAuditLog::deleteQuietly);
        } catch (IOException ignored) {
            // Retention is best-effort; an unwritable directory will still fail the append below.
        }
    }

    private static boolean isOlderThan(Path path, Instant cutoff) {
        try {
            return Files.isRegularFile(path)
                    && Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The next daily sweep will try again.
        }
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
