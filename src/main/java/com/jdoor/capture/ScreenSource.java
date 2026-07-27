package com.jdoor.capture;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface ScreenSource {
    Rectangle bounds();

    BufferedImage capture();
}
