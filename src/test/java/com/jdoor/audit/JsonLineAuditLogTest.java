package com.jdoor.audit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonLineAuditLogTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void appendsEscapedEventsWithoutSecrets() throws Exception {
        Instant timestamp = Instant.parse("2026-07-26T12:30:00Z");
        JsonLineAuditLog log = new JsonLineAuditLog(temporaryDirectory, Clock.fixed(timestamp, ZoneOffset.UTC));

        log.record(new AuditEvent(timestamp, "viewer_connected", "127.0.0.1", "Viewer \"one\" approved"));
        log.record(new AuditEvent(timestamp.plusSeconds(1), "control_disabled", "127.0.0.1", "Changed by host"));

        Path file = temporaryDirectory.resolve("session-2026-07-26.jsonl");
        String contents = Files.readString(file);
        assertTrue(contents.contains("Viewer \\\"one\\\" approved"));
        assertTrue(contents.contains("\"type\":\"control_disabled\""));
        assertTrue(contents.lines().count() == 2);
        assertFalse(contents.toLowerCase().contains("token"));
    }

    @Test
    void capsEachDailyLogFile() throws Exception {
        Instant timestamp = Instant.parse("2026-07-26T12:30:00Z");
        JsonLineAuditLog log = new JsonLineAuditLog(
                temporaryDirectory, Clock.fixed(timestamp, ZoneOffset.UTC), 512, Duration.ofDays(30));

        for (int index = 0; index < 20; index++) {
            log.record(new AuditEvent(timestamp, "connection_error", "192.0.2.1", "Rejected attempt " + index));
        }

        Path file = temporaryDirectory.resolve("session-2026-07-26.jsonl");
        assertTrue(Files.size(file) <= 512);
        assertNotEquals(20, Files.readAllLines(file).size());
    }

    @Test
    void removesOnlyExpiredOwnedLogFiles() throws Exception {
        Instant timestamp = Instant.parse("2026-07-26T12:30:00Z");
        Path expired = temporaryDirectory.resolve("session-2026-05-01.jsonl");
        Path unrelated = temporaryDirectory.resolve("notes.jsonl");
        Files.writeString(expired, "{}\n");
        Files.writeString(unrelated, "keep\n");
        FileTime old = FileTime.from(timestamp.minus(Duration.ofDays(60)));
        Files.setLastModifiedTime(expired, old);
        Files.setLastModifiedTime(unrelated, old);
        JsonLineAuditLog log = new JsonLineAuditLog(
                temporaryDirectory, Clock.fixed(timestamp, ZoneOffset.UTC), 512, Duration.ofDays(30));

        log.record(new AuditEvent(timestamp, "host_started", "local", "Ready"));

        assertFalse(Files.exists(expired));
        assertTrue(Files.exists(unrelated));
    }
}
