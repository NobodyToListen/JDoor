package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

/**
 * Classe per la gestione del click del mouse.
 */
public class MouseSenderController implements MouseListener {
    private final ClientFrame clientFrame;
    private final ClientCommander commander;

    /**
     * Costruttore della classe.
     * @param clientFrame Il frame del client.
     * @param commander Il commander che manderà il comando del mouse.
     */
    public MouseSenderController(ClientFrame clientFrame, ClientCommander commander) {
        this.clientFrame = clientFrame;
        this.commander = commander;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        char button = 'L';
        if(e.getButton() != MouseEvent.BUTTON1) {
            button = 'R';
        }
        try {
            commander.sendMousePosition(e.getX(), e.getY(), button);
        } catch (IOException ex) {
            clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        clientFrame.setFocusable(true);
        clientFrame.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        clientFrame.setFocusable(false);
    }
}