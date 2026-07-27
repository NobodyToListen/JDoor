package com.jdoor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JFrame;

final class BrandIcon {
    private BrandIcon() {}

    static void apply(JFrame frame) {
        frame.setIconImages(List.of(image(16), image(24), image(32), image(48), image(64), image(128), image(256)));
    }

    private static BufferedImage image(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float scale = size / 64f;
            graphics.scale(scale, scale);
            graphics.setColor(new Color(0x15191F));
            graphics.fill(new RoundRectangle2D.Float(1, 1, 62, 62, 15, 15));
            graphics.setColor(Theme.ACCENT);
            graphics.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(new RoundRectangle2D.Float(18, 12, 30, 40, 4, 4));
            graphics.drawLine(29, 12, 29, 52);
            graphics.setColor(Theme.SUCCESS);
            graphics.fillOval(38, 30, 6, 6);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
