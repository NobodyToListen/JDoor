package com.jdoor.client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * JPanel per mostrare lo schermo del server.
 */
public class StreamView extends JPanel {
    private BufferedImage screen;

    /**
     * Costruttore del Thread.
     */
    public StreamView() {
        setBackground(Color.BLACK);
    }

    /**
     * Metodo per impostare i dati da mostrare a schermo.
     * @param image L'array di bytes che compongono l'immagine.
     * @throws IOException Se non si riuscisse a convertire l'array di bytes a immagine.
     */
    public void setScreen(byte[] image) throws IOException {
        ByteArrayInputStream imageConverter = new ByteArrayInputStream(image);
        screen = ImageIO.read(imageConverter);
        revalidate();
        repaint();
    }

    /**
     * Metodo per aggiornare l'immagine presente sul panel.
     * @param g Graphics (fornito di default).
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(screen != null) {
            g.drawImage(screen, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
