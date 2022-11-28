package com.jdoor.client.view;

import javax.swing.*;

public final class MainWindow extends JFrame {
    private JPanel mainPanel;

    public MainWindow() {
        setContentPane(mainPanel);

        setSize(700, 500);
        setResizable(false);
        setTitle("JDoor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
