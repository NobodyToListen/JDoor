package com.jdoor.session;

import java.io.IOException;

public final class ConnectionRejectedException extends IOException {
    private static final long serialVersionUID = 1L;

    public ConnectionRejectedException(String message) {
        super(message);
    }
}
