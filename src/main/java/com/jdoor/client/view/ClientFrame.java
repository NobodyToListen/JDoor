package com.jdoor.client.view;

import javax.swing.*;
import java.awt.*;

public final class ClientFrame extends JFrame{
    private JPanel mainPanel;
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton operationBtn;
    private JLabel inputLabel;
    private JPanel screenPanel;
    private JButton disconnectBtn;

    public ClientFrame() {
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setContentPane(mainPanel);
        setResizable(false);
        setFocusable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        disconnectBtn.setEnabled(false);
    }

    private void createUIComponents() {
        screenPanel = new ScreenView();
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
}
