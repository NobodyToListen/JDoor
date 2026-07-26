package com.jdoor.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jdoor.protocol.WireMessage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class RemoteScreenPanelTest {
    @Test
    void acceptsOrdinaryJpegFrames() throws IOException {
        RemoteScreenPanel panel = new RemoteScreenPanel();
        byte[] jpeg = encodedImage("jpeg");

        assertDoesNotThrow(() -> panel.setFrame(frame(32, 24, jpeg)));
    }

    @Test
    void rejectsNonJpegFrames() throws IOException {
        RemoteScreenPanel panel = new RemoteScreenPanel();

        assertThrows(IOException.class, () -> panel.setFrame(frame(32, 24, encodedImage("png"))));
    }

    @Test
    void rejectsJpegDimensionsBeforeDecodingAnOversizedRaster() throws IOException {
        RemoteScreenPanel panel = new RemoteScreenPanel();
        byte[] jpeg = encodedImage("jpeg");
        overwriteStartOfFrameDimensions(jpeg, 0xffff, 0xffff);

        assertThrows(IOException.class, () -> panel.setFrame(frame(32, 24, jpeg)));
    }

    @Test
    void rejectsFramesWhoseDeclaredAndEncodedDimensionsDiffer() throws IOException {
        RemoteScreenPanel panel = new RemoteScreenPanel();

        assertThrows(IOException.class, () -> panel.setFrame(frame(31, 24, encodedImage("jpeg"))));
    }

    private static byte[] encodedImage(String format) throws IOException {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IOException("Missing " + format + " test encoder");
        }
        return output.toByteArray();
    }

    private static WireMessage.ScreenFrame frame(int width, int height, byte[] jpeg) {
        return new WireMessage.ScreenFrame(0, 0, width, height, jpeg);
    }

    private static void overwriteStartOfFrameDimensions(byte[] jpeg, int width, int height) throws IOException {
        for (int index = 2; index + 8 < jpeg.length; ) {
            if ((jpeg[index] & 0xff) != 0xff) {
                index++;
                continue;
            }
            int marker = jpeg[index + 1] & 0xff;
            if (marker == 0xc0 || marker == 0xc1 || marker == 0xc2) {
                jpeg[index + 5] = (byte) (height >>> 8);
                jpeg[index + 6] = (byte) height;
                jpeg[index + 7] = (byte) (width >>> 8);
                jpeg[index + 8] = (byte) width;
                return;
            }
            if (marker == 0xd8 || marker == 0xd9 || (marker >= 0xd0 && marker <= 0xd7)) {
                index += 2;
                continue;
            }
            if (index + 3 >= jpeg.length) {
                break;
            }
            int segmentLength = ((jpeg[index + 2] & 0xff) << 8) | (jpeg[index + 3] & 0xff);
            if (segmentLength < 2) {
                break;
            }
            index += segmentLength + 2;
        }
        throw new IOException("JPEG test fixture has no start-of-frame marker");
    }
}
