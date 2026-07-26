package com.jdoor.session;

import com.jdoor.protocol.WireMessage;

public interface ViewerEventListener {
    default void onConnected(WireMessage.ServerHello hello) {}

    default void onScreenFrame(WireMessage.ScreenFrame frame) {}

    default void onControlChanged(boolean enabled) {}

    default void onDisconnected(String reason) {}

    default void onError(String message, Throwable cause) {}
}
