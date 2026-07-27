package com.jdoor.control;

import com.jdoor.protocol.WireMessage;

public interface RemoteInputController {
    void apply(WireMessage.PointerInput input);

    void apply(WireMessage.KeyboardInput input);

    void releaseAll();
}
