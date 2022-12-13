package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Classe per la gestione della finestra principale.
 */
public class WindowManagement implements WindowListener {

    private final ClientFrame clientFrame;
    private final ClientCommander commander;

    /**
     * Costruttore della classe.
     * @param clientFrame Il frame del client.
     * @param commander il commander che manderà il comando quando si riduce a icona o si massimizza la finestra.
     */
    public WindowManagement(ClientFrame clientFrame, ClientCommander commander) {
        this.clientFrame = clientFrame;
        this.commander = commander;
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            try {
                commander.closeConnection();
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            try {
                commander.sendScreenStopStart();
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            try {
                commander.sendScreenStopStart();
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
