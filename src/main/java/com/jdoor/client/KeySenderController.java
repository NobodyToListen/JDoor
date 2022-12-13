package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Classe per la gestione dell'invio dei tasti al server.
 */
public class KeySenderController implements KeyListener {
    private final ClientFrame clientFrame;
    private final ClientCommander commander;

    /**
     * Costruttore
     * @param clientFrame il frame del client.
     * @param commander il commander che manderà il comando del tasto.
     */
    public KeySenderController(ClientFrame clientFrame, ClientCommander commander) {
        this.clientFrame = clientFrame;
        this.commander = commander;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            commander.sendKey(e.getKeyCode());
            System.out.println("KEY:" + e.getKeyCode() + " sent" + "\n");
        } catch (Exception ex) {
            clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
