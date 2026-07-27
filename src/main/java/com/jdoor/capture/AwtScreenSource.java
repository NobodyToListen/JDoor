package com.jdoor.capture;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public final class AwtScreenSource implements ScreenSource {
    private final Robot robot;
    private final Rectangle bounds;

    public AwtScreenSource() throws AWTException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException("Screen sharing requires a graphical desktop session");
        }
        GraphicsDevice device =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        bounds = new Rectangle(device.getDefaultConfiguration().getBounds());
        robot = new Robot(device);
        robot.setAutoDelay(2);
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(bounds);
    }

    @Override
    public BufferedImage capture() {
        return robot.createScreenCapture(bounds);
    }
}
