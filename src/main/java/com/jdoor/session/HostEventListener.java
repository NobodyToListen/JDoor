package com.jdoor.session;

import com.jdoor.security.PairingLink;

public interface HostEventListener {
    default void onPairingLinkChanged(PairingLink link) {}

    default void onViewerConnected(String displayName, String remoteAddress) {}

    default void onViewerDisconnected(String reason) {}

    default void onControlChanged(boolean enabled) {}

    default void onActivity(String message) {}

    default void onError(String message, Throwable cause) {}
}
