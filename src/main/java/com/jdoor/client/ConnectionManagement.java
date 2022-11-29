package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jdoor.Constants.*;

public class ConnectionManagement implements ActionListener {
    private ClientFrame clientFrame;
    private ClientCommander commander;
    private KeySenderController keySenderController;
    private MouseSenderController mouseSenderController;
    private WindowManagement windowManagement;
    private FileOperationsController fileOperationsController;

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
                        commander = new ClientCommander(ip, TCP_PORT, UDP_PORT,clientFrame);
                        clientFrame.getOperationBtn().setText("SEND");
                        clientFrame.getDiconnectBtn().setEnabled(true);
                        clientFrame.getInputLabel().setText("CMD");

                        keySenderController = new KeySenderController(clientFrame,commander);
                        mouseSenderController = new MouseSenderController(clientFrame,commander);
                        windowManagement = new WindowManagement(clientFrame,commander);
                        fileOperationsController = new FileOperationsController(clientFrame,commander);

                        clientFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        clientFrame.getScreenPanel().addMouseListener(mouseSenderController);
                        clientFrame.addKeyListener(keySenderController);
                        clientFrame.addWindowListener(windowManagement);
                        clientFrame.getGetFileBtn().addActionListener(fileOperationsController);
                        clientFrame.getSendFileBtn().addActionListener(fileOperationsController);


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
                commander.closeConnection();
                clientFrame.getOperationBtn().setText("CONNECT");
                clientFrame.getDiconnectBtn().setEnabled(false);
                clientFrame.getInputLabel().setText("HOST");

                clientFrame.getScreenPanel().removeMouseListener(mouseSenderController);
                clientFrame.removeKeyListener(keySenderController);
                clientFrame.removeWindowListener(windowManagement);
                clientFrame.getGetFileBtn().removeActionListener(fileOperationsController);
                clientFrame.getSendFileBtn().removeActionListener(fileOperationsController);

                clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
        clientFrame.getOutputArea().setText("");
    }
}
