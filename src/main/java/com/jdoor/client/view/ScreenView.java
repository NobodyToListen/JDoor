package com.jdoor.client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.jdoor.server.Server.IMAGE_UDP_SIZE;

public class ScreenView extends JPanel{
    private BufferedImage screen;
    public ScreenView() {
        setBackground(Color.BLACK);
    }


    public void setScreen(byte[] image) throws IOException {
        ByteArrayInputStream imageConverter = new ByteArrayInputStream(image);
        byte[] compressedImage = imageConverter.readAllBytes();

        Inflater inflater = new Inflater();
        inflater.setInput(compressedImage);

        byte[] originalImage = new byte[IMAGE_UDP_SIZE];
        try {
            inflater.inflate(originalImage);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }

        ByteArrayInputStream newImage = new ByteArrayInputStream(originalImage);

        screen = ImageIO.read(newImage);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(screen != null) {
            System.out.println("Sto disegnando\n");
            g.drawImage(screen, 0, 0, getWidth(), getHeight(), null);
        }
    }
}
