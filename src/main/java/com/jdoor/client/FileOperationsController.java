package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static com.jdoor.Constants.whereToDownloadFiles;

public class FileOperationsController implements ActionListener {
    private ClientFrame clientFrame;
    private ClientCommander commander;

    public FileOperationsController(ClientFrame clientFrame, ClientCommander commander) {
        this.clientFrame = clientFrame;
        this.commander = commander;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String filePath = clientFrame.getFileLocationField().getText();
        if(e.getSource() == clientFrame.getSendFileBtn()) {
            try {
                commander.sendFile(filePath);
            } catch (IOException ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        } else {
            try {
                commander.getFile(whereToDownloadFiles, filePath);
            } catch (IOException ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
    }
}
