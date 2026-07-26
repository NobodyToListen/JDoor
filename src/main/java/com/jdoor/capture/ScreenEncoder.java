package com.jdoor.capture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class ScreenEncoder {
    private final int maximumWidth;
    private final int maximumHeight;
    private final float jpegQuality;

    public ScreenEncoder(int maximumWidth, int maximumHeight, float jpegQuality) {
        if (maximumWidth < 320 || maximumWidth > 7_680) {
            throw new IllegalArgumentException("maximumWidth is outside the supported range");
        }
        if (maximumHeight < 240 || maximumHeight > 4_320) {
            throw new IllegalArgumentException("maximumHeight is outside the supported range");
        }
        if (!Float.isFinite(jpegQuality) || jpegQuality < 0.25f || jpegQuality > 0.95f) {
            throw new IllegalArgumentException("jpegQuality must be between 0.25 and 0.95");
        }
        this.maximumWidth = maximumWidth;
        this.maximumHeight = maximumHeight;
        this.jpegQuality = jpegQuality;
    }

    public EncodedScreen encode(BufferedImage source) throws IOException {
        Objects.requireNonNull(source, "source");
        BufferedImage scaled = scale(source);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG encoder is available");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(jpegQuality);
            }
            writer.setOutput(output);
            writer.write(null, new IIOImage(scaled, null, null), parameters);
            output.flush();
            return new EncodedScreen(scaled.getWidth(), scaled.getHeight(), bytes.toByteArray());
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage scale(BufferedImage source) {
        double scale = Math.min(
                1.0d, Math.min((double) maximumWidth / source.getWidth(), (double) maximumHeight / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    public record EncodedScreen(int width, int height, byte[] jpeg) {
        public EncodedScreen {
            jpeg = Objects.requireNonNull(jpeg, "jpeg").clone();
            if (width < 1 || height < 1 || jpeg.length == 0) {
                throw new IllegalArgumentException("Encoded screen is empty");
            }
        }

        @Override
        public byte[] jpeg() {
            return jpeg.clone();
        }
    }
}
