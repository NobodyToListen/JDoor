package com.jdoor.client.view;

import javax.swing.*;

public class MainWindow extends JFrame {
    private JPanel mainPanel;
    private JTextField ipField;
    private JButton connectBtn;
    private JPanel screenPanel;

    public MainWindow() {
        setContentPane(mainPanel);

        setSize(700, 500);
        setResizable(false);
        setTitle("JDoor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
