package com.jdoor.audit;

import java.time.Instant;
import java.util.Objects;

public record AuditEvent(Instant timestamp, String type, String remoteAddress, String detail) {
    public AuditEvent {
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        type = sanitize(type, "type", 64);
        remoteAddress = sanitize(remoteAddress, "remoteAddress", 128);
        detail = sanitize(detail, "detail", 512);
    }

    private static String sanitize(String value, String field, int maximumLength) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty() || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " is empty or too long");
        }
        return value.replaceAll("\\p{Cntrl}", " ");
    }
}
