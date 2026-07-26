package com.jdoor.audit;

import java.io.IOException;

@FunctionalInterface
public interface AuditLog {
    void record(AuditEvent event) throws IOException;

    static AuditLog noOp() {
        return event -> {};
    }
}
