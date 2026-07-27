package com.jdoor.control;

import com.jdoor.protocol.WireMessage;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.HashSet;
import java.util.Set;

public final class AwtRemoteInputController implements RemoteInputController {
    private final Robot robot;
    private final Rectangle screenBounds;
    private final InputPolicy policy;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> pressedButtons = new HashSet<>();

    public AwtRemoteInputController(Rectangle screenBounds) throws AWTException {
        this(new Robot(), screenBounds, InputPolicy.DEFAULT);
    }

    AwtRemoteInputController(Robot robot, Rectangle screenBounds, InputPolicy policy) {
        this.robot = robot;
        this.screenBounds = new Rectangle(screenBounds);
        this.policy = policy;
        robot.setAutoDelay(2);
    }

    @Override
    public synchronized void apply(WireMessage.PointerInput input) {
        int x = screenBounds.x
                + Math.min(screenBounds.width - 1, Math.round(input.normalizedX() * (screenBounds.width - 1)));
        int y = screenBounds.y
                + Math.min(screenBounds.height - 1, Math.round(input.normalizedY() * (screenBounds.height - 1)));
        robot.mouseMove(x, y);
        if (input.action() == WireMessage.PointerAction.MOVE || !policy.allowsMouseButton(input.button())) {
            return;
        }
        int mask = buttonMask(input.button());
        if (input.action() == WireMessage.PointerAction.PRESS && pressedButtons.add(input.button())) {
            robot.mousePress(mask);
        } else if (input.action() == WireMessage.PointerAction.RELEASE && pressedButtons.remove(input.button())) {
            robot.mouseRelease(mask);
        }
    }

    @Override
    public synchronized void apply(WireMessage.KeyboardInput input) {
        if (!policy.allowsKey(input.keyCode())) {
            return;
        }
        try {
            if (input.action() == WireMessage.KeyAction.PRESS && pressedKeys.add(input.keyCode())) {
                robot.keyPress(input.keyCode());
            } else if (input.action() == WireMessage.KeyAction.RELEASE && pressedKeys.remove(input.keyCode())) {
                robot.keyRelease(input.keyCode());
            }
        } catch (IllegalArgumentException unsupportedKey) {
            pressedKeys.remove(input.keyCode());
        }
    }

    @Override
    public synchronized void releaseAll() {
        for (int keyCode : Set.copyOf(pressedKeys)) {
            try {
                robot.keyRelease(keyCode);
            } catch (IllegalArgumentException ignored) {
                // The platform does not expose this key to Robot.
            }
        }
        pressedKeys.clear();
        for (int button : Set.copyOf(pressedButtons)) {
            robot.mouseRelease(buttonMask(button));
        }
        pressedButtons.clear();
    }

    private static int buttonMask(int button) {
        return switch (button) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> throw new IllegalArgumentException("Unsupported mouse button");
        };
    }
}
