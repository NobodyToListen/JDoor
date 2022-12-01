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
    private JPanel webcamPanel;

    public ClientFrame() {
        setContentPane(mainPanel);
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("JDOOR");
        setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
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

    public JPanel getWebcamPanel() {
        return webcamPanel;
    }
    public JButton getDiconnectBtn() {
        return diconnectBtn;
    }
}
