package com.jdoor.client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ScreenView extends JPanel{
    private BufferedImage screen;
    public ScreenView() {
        setBackground(Color.BLACK);
    }


    public void setScreen(byte[] image) throws IOException {
        ByteArrayInputStream imageConverter = new ByteArrayInputStream(image);
        screen = ImageIO.read(imageConverter);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(screen != null) {
            //System.out.println("Sto disegnando\n");
            g.drawImage(screen, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
