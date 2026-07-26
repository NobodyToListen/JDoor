package com.jdoor.session;

import java.net.InetAddress;
import java.util.Objects;

public record ConnectionRequest(String displayName, InetAddress remoteAddress, String verificationCode) {
    public ConnectionRequest {
        displayName = Objects.requireNonNull(displayName, "displayName");
        remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
        verificationCode = Objects.requireNonNull(verificationCode, "verificationCode");
    }
}
