package com.jdoor.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ScreenEncoderTest {
    @Test
    void downscalesWhilePreservingAspectRatio() throws Exception {
        BufferedImage source = image(1920, 1080, Color.ORANGE);
        ScreenEncoder encoder = new ScreenEncoder(800, 600, 0.7f);

        ScreenEncoder.EncodedScreen encoded = encoder.encode(source);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded.jpeg()));

        assertEquals(800, encoded.width());
        assertEquals(450, encoded.height());
        assertNotNull(decoded);
        assertEquals(800, decoded.getWidth());
        assertEquals(450, decoded.getHeight());
        assertTrue(encoded.jpeg().length > 100);
    }

    @Test
    void neverUpscalesAndConvertsAlphaToJpegCompatibleRgb() throws Exception {
        BufferedImage source = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(new Color(10, 20, 30, 100));
        graphics.fillRect(0, 0, 320, 240);
        graphics.dispose();

        ScreenEncoder.EncodedScreen encoded = new ScreenEncoder(1600, 900, 0.65f).encode(source);

        assertEquals(320, encoded.width());
        assertEquals(240, encoded.height());
        assertNotNull(ImageIO.read(new ByteArrayInputStream(encoded.jpeg())));
    }

    private static BufferedImage image(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }
}
