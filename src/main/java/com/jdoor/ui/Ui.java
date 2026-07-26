package com.jdoor.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

final class Ui {
    private Ui() {}

    static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        return panel;
    }

    static JLabel heading(String text, int size) {
        JLabel label = plain(text);
        label.setForeground(Theme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        return label;
    }

    static JLabel muted(String text) {
        JLabel label = plain(text);
        label.setForeground(Theme.MUTED);
        return label;
    }

    static JLabel plain(String text) {
        JLabel label = new JLabel();
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setText(text);
        return label;
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(11, 18, 11, 18));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBackground(primary ? Theme.ACCENT : Theme.SURFACE_RAISED);
        button.setForeground(primary ? Color.BLACK : Theme.TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? Theme.ACCENT : Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        return button;
    }

    static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true), BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    static void accessible(JComponent component, String name, String description) {
        component.getAccessibleContext().setAccessibleName(name);
        component.getAccessibleContext().setAccessibleDescription(description);
    }
}
