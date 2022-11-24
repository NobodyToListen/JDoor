package com.jdoor.server.mouse;

import java.awt.*;
import java.awt.event.InputEvent;

public class MouseController {
    private static MouseController currentInstance;
    private Robot robot;

    private MouseController() throws AWTException {
        currentInstance = new MouseController();
        robot = new Robot();
    }

    public static synchronized MouseController getInstance() throws AWTException {
        if (currentInstance == null)
            currentInstance = new MouseController();
        return currentInstance;
    }

    public synchronized void clickMouse(String command) {
        char direction = command.charAt(1);

        String rawCords = command.substring(2);
        String[] cords = rawCords.split(";");

        int x = Integer.getInteger(cords[0]);
        int y = Integer.getInteger(cords[1]);

        robot.mouseMove(x, y);
        // MR156;56 Esempio comando.
        if (direction == 'R') {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        } else {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }
}
