package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeySenderController implements KeyListener {
    private final ClientFrame clientFrame;
    private final ClientCommander commander;

    public KeySenderController(ClientFrame clientFrame, ClientCommander commander) {
        this.clientFrame = clientFrame;
        this.commander = commander;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

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
    public void keyReleased(KeyEvent e) {

    }
}
