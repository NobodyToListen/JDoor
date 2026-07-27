package com.jdoor.ui;

import com.jdoor.AppInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class LauncherFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public LauncherFrame() {
        super(AppInfo.NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 560));
        setSize(920, 620);
        setLocationRelativeTo(null);
        BrandIcon.apply(this);
        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(42, 52, 32, 52));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel eyebrow = Ui.muted("SECURE • VISIBLE • CONSENT-FIRST");
        eyebrow.setFont(eyebrow.getFont().deriveFont(12f));
        JLabel title = Ui.heading("Help someone, without hiding anything.", 32);
        JLabel subtitle = Ui.muted("Encrypted screen sharing for trusted local networks. "
                + "The host approves every viewer and controls every permission.");
        subtitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(10));
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridBagLayout());
        choices.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(34, 0, 20, 12);
        choices.add(
                choiceCard(
                        "Share this screen",
                        "Start a visible session, approve the viewer, then decide whether to allow control.",
                        "Start hosting",
                        true,
                        this::openHost),
                constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(34, 12, 20, 0);
        choices.add(
                choiceCard(
                        "Join a session",
                        "Paste the one-time link supplied by the person whose screen you are helping with.",
                        "Join with a link",
                        false,
                        this::openViewer),
                constraints);
        root.add(choices, BorderLayout.CENTER);

        JLabel footer = new JLabel("No unattended access • No remote shell • No telemetry", SwingConstants.CENTER);
        footer.setForeground(Theme.MUTED);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel choiceCard(String title, String description, String action, boolean primary, Runnable handler) {
        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel titleLabel = Ui.heading(title, 22);
        JLabel descriptionLabel =
                new JLabel("<html><div style='width:280px; line-height:1.45'>" + description + "</div></html>");
        descriptionLabel.setForeground(Theme.MUTED);
        JButton button = Ui.button(action, primary);
        button.addActionListener(event -> handler.run());
        Ui.accessible(button, action, description);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(descriptionLabel);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(30));
        card.add(button);
        return card;
    }

    private void openHost() {
        dispose();
        new HostFrame(AppInfo.DEFAULT_PORT).setVisible(true);
    }

    private void openViewer() {
        dispose();
        new ViewerFrame(null).setVisible(true);
    }
}
