package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClient implements MouseListener, KeyListener, WindowListener, ActionListener {
    private ClientFrame clientFrame;
    private ClientCommander commander;
    private final int TCP_PORT = 8080;
    private final int UDP_PORT = 8081;

    public MainClient(ClientFrame clientFrame) {
        this.clientFrame = clientFrame;
        this.clientFrame.addWindowListener(this);
        this.clientFrame.getOperationBtn().addActionListener(this);
        this.clientFrame.getDiconnectBtn().addActionListener(this);
    }

    private boolean isValidIP(String ip) {

        String zeroTo255
                = "(\\d{1,2}|(0|1)\\"
                + "d{2}|2[0-4]\\d|25[0-5])";


        String regex
                = zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255;

        Pattern p = Pattern.compile(regex);

        if (ip == null) {
            return false;
        }

        if (ip.equals("localhost")) {
            return true;
        }

        Matcher m = p.matcher(ip);
        return m.matches();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == clientFrame.getOperationBtn()) {
            if(clientFrame.getOperationBtn().getText().equals("CONNECT")) {
                String ip = clientFrame.getInputField().getText();
                if(isValidIP(ip)) {
                    try {
                        commander = new ClientCommander(ip, TCP_PORT, UDP_PORT,clientFrame);
                        clientFrame.getScreenPanel().addMouseListener(this);
                        clientFrame.addKeyListener(this);
                        clientFrame.getOperationBtn().setText("SEND");
                        clientFrame.getDiconnectBtn().setEnabled(true);
                        clientFrame.getInputLabel().setText("CMD");
                        commander.start();
                    } catch (IOException ex) {
                        clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                    }
                } else {
                    clientFrame.getOutputArea().setText("ERROR: Invalid ip\n");
                }
            } else {
                try {
                    commander.sendCommands(clientFrame.getInputField().getText());
                    System.out.println("COMMAND:" + clientFrame.getInputField().getText() + " sent" + "\n");
                } catch (IOException ex) {
                    clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                }
            }
        } else {
            try {
                commander.closeConnection();
                clientFrame.getScreenPanel().removeMouseListener(this);
                clientFrame.removeKeyListener(this);
                clientFrame.getOperationBtn().setText("CONNECT");
                clientFrame.getDiconnectBtn().setEnabled(false);
                clientFrame.getInputLabel().setText("HOST");
            } catch (IOException ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }

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
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        clientFrame.setFocusable(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        clientFrame.setFocusable(false);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            commander.sendKey(e.getKeyCode());
            System.out.println("KEY:" + e.getKeyCode() + " sent" + "\n");
        } catch (IOException ex) {
            clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            if(commander != null && commander.getSocketCommands() != null) {
                    try {
                        commander.closeConnection();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
            }
            System.exit(0);
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public static void main(String[] args) {
        ClientFrame cFrame = new ClientFrame();
        MainClient main = new MainClient(cFrame);
    }
}
