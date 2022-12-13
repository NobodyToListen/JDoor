package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class WindowManagement implements WindowListener {

    private ClientFrame clientFrame;
    private ClientCommander commander;

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
