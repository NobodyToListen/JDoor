package com.jdoor.server.keyboard;

import java.awt.*;

public class KeyboardController {
    private static KeyboardController currentInstance;

    private final Robot robot;

    private KeyboardController() throws AWTException {
        robot = new Robot();
    }

    public static synchronized KeyboardController getInstance() throws AWTException {
        if (currentInstance == null)
            currentInstance = new KeyboardController();
        return currentInstance;
    }

    public synchronized void pressKeyboard(String key) {
        //k4 -> Key 4
        String code = key.substring(1);
        int intCode;
        try {
            intCode = Integer.parseInt(code);
        } catch (Exception e) {
            intCode = 0;
        }

        robot.keyPress(intCode);
        robot.keyRelease(intCode);
    }
}
