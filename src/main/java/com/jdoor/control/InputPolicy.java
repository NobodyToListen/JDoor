package com.jdoor.control;

import java.awt.event.KeyEvent;
import java.util.Set;

public final class InputPolicy {
    public static final InputPolicy DEFAULT = new InputPolicy(Set.of(
            KeyEvent.VK_WINDOWS,
            KeyEvent.VK_META,
            KeyEvent.VK_CONTEXT_MENU,
            KeyEvent.VK_PRINTSCREEN,
            KeyEvent.VK_PAUSE));

    private final Set<Integer> blockedKeys;

    public InputPolicy(Set<Integer> blockedKeys) {
        this.blockedKeys = Set.copyOf(blockedKeys);
    }

    public boolean allowsKey(int keyCode) {
        return keyCode >= KeyEvent.VK_BACK_SPACE && keyCode <= KeyEvent.VK_CUT && !blockedKeys.contains(keyCode);
    }

    public boolean allowsMouseButton(int button) {
        return button >= 1 && button <= 3;
    }
}
