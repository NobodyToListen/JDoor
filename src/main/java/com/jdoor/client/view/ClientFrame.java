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
    private JButton diconnectBtn;

    public ClientFrame() {
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setContentPane(mainPanel);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        diconnectBtn.setEnabled(false);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
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

    public JPanel getOutputPanel() {
        return outputPanel;
    }

    public JPanel getScreenPanel() {
        return screenPanel;
    }

    public JButton getDiconnectBtn() {
        return diconnectBtn;
    }
}
