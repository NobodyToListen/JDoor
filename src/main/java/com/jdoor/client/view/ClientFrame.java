package com.jdoor.client.view;

import javax.swing.*;
import java.awt.*;

public class ClientFrame extends JFrame{
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton operationBtn;
    private JLabel inputLabel;
    private JPanel outputPanel;
    private JPanel screenPanel;

    private JButton disconnectBtn;

    private JPanel webcamPanel;


    public ClientFrame() {
        setContentPane(mainPanel);
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("JDOOR");
        setVisible(true);

        disconnectBtn.setEnabled(false);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        mainPanel = new JPanel();
        screenPanel = new StreamView();
        webcamPanel = new StreamView();
    }

    public JTextArea getOutputArea() {
        return outputArea;
    }

    public JTextField getInputField() {
        return inputField;
    }

    public JButton getOperationBtn() {
        return operationBtn;
    }

    public JLabel getInputLabel() {
        return inputLabel;
    }

    public JPanel getScreenPanel() {
        return screenPanel;
    }

    public JButton getDisconnectBtn() {
        return disconnectBtn;
    }

    public JPanel getWebcamPanel() {
        return webcamPanel;
    }
}
