package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.jdoor.Constants.*;

public class ConnectionManagement implements ActionListener {
    private ClientFrame clientFrame;
    private ClientCommander commander;
    private KeySenderController keySenderController;
    private MouseSenderController mouseSenderController;
    private WindowManagement windowManagement;

    public ConnectionManagement(ClientFrame clientFrame) {
        this.clientFrame = clientFrame;
        this.clientFrame.getOperationBtn().addActionListener(this);
        this.clientFrame.getDiconnectBtn().addActionListener(this);
        this.clientFrame.getDiconnectBtn().setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == clientFrame.getOperationBtn()) {
            if(clientFrame.getOperationBtn().getText().equals("CONNECT")) {
                String ip = clientFrame.getInputField().getText();
                if(isValidIP(ip)) {
                    try {
                        commander = new ClientCommander(ip,clientFrame);
                        clientFrame.getOperationBtn().setText("SEND");
                        clientFrame.getDiconnectBtn().setEnabled(true);
                        clientFrame.getInputLabel().setText("CMD");

                        keySenderController = new KeySenderController(clientFrame,commander);
                        mouseSenderController = new MouseSenderController(clientFrame,commander);
                        windowManagement = new WindowManagement(clientFrame,commander);

                        clientFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        clientFrame.getScreenPanel().addMouseListener(mouseSenderController);
                        clientFrame.addKeyListener(keySenderController);
                        clientFrame.addWindowListener(windowManagement);


                        commander.start();
                    } catch (Exception ex) {
                        clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                    }
                } else {
                    clientFrame.getOutputArea().setText("ERROR: Invalid ip\n");
                }
            } else {
                try {
                    commander.sendCommands(clientFrame.getInputField().getText());
                    System.out.println("COMMAND:" + clientFrame.getInputField().getText() + " sent" + "\n");
                } catch (Exception ex) {
                    clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                }
            }
        } else {
            try {
                if(commander.getSocketCommands() != null) {
                    commander.closeConnection();
                }
                clientFrame.getOperationBtn().setText("CONNECT");
                clientFrame.getDiconnectBtn().setEnabled(false);
                clientFrame.getInputLabel().setText("HOST");

                clientFrame.getScreenPanel().removeMouseListener(mouseSenderController);
                clientFrame.removeKeyListener(keySenderController);
                clientFrame.removeWindowListener(windowManagement);

                clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
        clientFrame.getInputField().setText("");
    }
}
