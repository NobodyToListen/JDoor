package com.jdoor.ui;

import com.jdoor.protocol.FrameLimits;
import com.jdoor.protocol.WireMessage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JPanel;

final class RemoteScreenPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private transient volatile BufferedImage screen;

    RemoteScreenPanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        Ui.accessible(
                this,
                "Shared screen",
                "The approved host screen. Remote input works only when the host enables control.");
    }

    void setFrame(WireMessage.ScreenFrame frame) throws IOException {
        Objects.requireNonNull(frame, "frame");
        BufferedImage decoded = decodeBoundedJpeg(frame.jpeg());
        if (decoded.getWidth() != frame.width() || decoded.getHeight() != frame.height()) {
            throw new IOException("The host sent a screen frame with mismatched dimensions");
        }
        screen = decoded;
        repaint();
    }

    private static BufferedImage decodeBoundedJpeg(byte[] jpeg) throws IOException {
        Objects.requireNonNull(jpeg, "jpeg");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(jpeg))) {
            if (input == null) {
                throw new IOException("The host sent an unreadable screen frame");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("The host sent an unreadable screen frame");
            }
            ImageReader reader = readers.next();
            try {
                if (!"JPEG".equalsIgnoreCase(reader.getFormatName())) {
                    throw new IOException("The host sent a screen frame that is not JPEG");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IOException("The host sent an unreadable screen frame");
                }
                validateDimensions(decoded.getWidth(), decoded.getHeight());
                return decoded;
            } finally {
                reader.dispose();
            }
        }
    }

    private static void validateDimensions(int width, int height) throws IOException {
        if (!FrameLimits.isSafeDimensions(width, height)) {
            throw new IOException("The host sent a screen frame with unsafe dimensions");
        }
    }

    Optional<NormalizedPoint> normalizedPoint(int x, int y) {
        return normalizedPoint(x, y, false);
    }

    Optional<NormalizedPoint> normalizedPointClamped(int x, int y) {
        return normalizedPoint(x, y, true);
    }

    private Optional<NormalizedPoint> normalizedPoint(int x, int y, boolean clamp) {
        BufferedImage current = screen;
        if (current == null || getWidth() < 2 || getHeight() < 2) {
            return Optional.empty();
        }
        double scale = Math.min((double) getWidth() / current.getWidth(), (double) getHeight() / current.getHeight());
        int renderedWidth = Math.max(1, (int) Math.round(current.getWidth() * scale));
        int renderedHeight = Math.max(1, (int) Math.round(current.getHeight() * scale));
        int left = (getWidth() - renderedWidth) / 2;
        int top = (getHeight() - renderedHeight) / 2;
        if (!clamp && (x < left || x >= left + renderedWidth || y < top || y >= top + renderedHeight)) {
            return Optional.empty();
        }
        x = Math.max(left, Math.min(left + renderedWidth - 1, x));
        y = Math.max(top, Math.min(top + renderedHeight - 1, y));
        float normalizedX = renderedWidth == 1 ? 0f : (float) (x - left) / (renderedWidth - 1);
        float normalizedY = renderedHeight == 1 ? 0f : (float) (y - top) / (renderedHeight - 1);
        return Optional.of(new NormalizedPoint(normalizedX, normalizedY));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            BufferedImage current = screen;
            if (current == null) {
                canvas.setColor(Theme.MUTED);
                canvas.setFont(getFont().deriveFont(Font.PLAIN, 16f));
                String message = "Waiting for the first encrypted frame…";
                int width = canvas.getFontMetrics().stringWidth(message);
                canvas.drawString(message, Math.max(20, (getWidth() - width) / 2), Math.max(30, getHeight() / 2));
                return;
            }
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale =
                    Math.min((double) getWidth() / current.getWidth(), (double) getHeight() / current.getHeight());
            int width = Math.max(1, (int) Math.round(current.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(current.getHeight() * scale));
            int left = (getWidth() - width) / 2;
            int top = (getHeight() - height) / 2;
            canvas.drawImage(current, left, top, width, height, null);
        } finally {
            canvas.dispose();
        }
    }

    record NormalizedPoint(float x, float y) {}
}
