package com.jdoor.ui;

import com.jdoor.AppInfo;
import com.jdoor.audit.JsonLineAuditLog;
import com.jdoor.capture.AwtScreenSource;
import com.jdoor.control.AwtRemoteInputController;
import com.jdoor.security.PairingLink;
import com.jdoor.session.ConnectionRequest;
import com.jdoor.session.HostConfiguration;
import com.jdoor.session.HostEventListener;
import com.jdoor.session.HostSessionServer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class HostFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter ACTIVITY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int port;
    private final String advertisedHost;
    private final JLabel statusLabel = new JLabel("Starting securely…");
    private final JTextArea pairingLinkArea = new JTextArea();
    private final JLabel verificationCodeLabel = new JLabel("—");
    private final JTextArea activityArea = new JTextArea();
    private final JButton copyButton = Ui.button("Copy one-time link", true);
    private final JButton disconnectButton = Ui.button("Disconnect viewer", false);
    private final JToggleButton controlToggle = new JToggleButton("Allow remote control");
    private final JButton stopButton = Ui.button("Stop sharing", false);

    private transient volatile HostSessionServer server;
    private transient volatile SwingWorker<StartedHost, Void> startupWorker;
    private volatile boolean closing;

    public HostFrame(int port) {
        this(port, null);
    }

    public HostFrame(int port, String advertisedHost) {
        super(AppInfo.NAME + " — Host");
        this.port = port;
        this.advertisedHost = advertisedHost;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(940, 650));
        setSize(1_080, 720);
        setLocationRelativeTo(null);
        BrandIcon.apply(this);
        setContentPane(buildContent());
        configureActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                stopAndExit();
            }
        });
        SwingUtilities.invokeLater(this::startHostAsync);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 24));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel eyebrow = Ui.muted("HOST SESSION");
        JLabel title = Ui.heading("Your screen stays under your control.", 28);
        titleBlock.add(eyebrow);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(title);
        header.add(titleBlock, BorderLayout.WEST);
        statusLabel.setForeground(Theme.WARNING);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true), BorderFactory.createEmptyBorder(9, 13, 9, 13)));
        header.add(statusLabel, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.weightx = 0.58;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 12);
        content.add(buildPairingCard(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.42;
        constraints.insets = new Insets(0, 12, 0, 0);
        content.add(buildActivityCard(), constraints);
        root.add(content, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        JPanel permissionGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        permissionGroup.setOpaque(false);
        controlToggle.setEnabled(false);
        controlToggle.setBackground(Theme.SURFACE_RAISED);
        controlToggle.setForeground(Theme.TEXT);
        controlToggle.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        disconnectButton.setEnabled(false);
        permissionGroup.add(controlToggle);
        permissionGroup.add(disconnectButton);
        controls.add(permissionGroup, BorderLayout.WEST);
        controls.add(stopButton, BorderLayout.EAST);
        root.add(controls, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildPairingCard() {
        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Ui.heading("Invite one trusted viewer", 21));
        card.add(Box.createVerticalStrut(8));
        JLabel explanation = Ui.muted("Send this link through a channel you trust. It expires after 10 minutes "
                + "and becomes invalid as soon as you approve someone.");
        card.add(explanation);
        card.add(Box.createVerticalStrut(20));

        pairingLinkArea.setEditable(false);
        pairingLinkArea.setLineWrap(true);
        pairingLinkArea.setWrapStyleWord(false);
        pairingLinkArea.setRows(5);
        pairingLinkArea.setBackground(Theme.SURFACE_RAISED);
        pairingLinkArea.setForeground(Theme.TEXT);
        pairingLinkArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        pairingLinkArea.setBorder(Ui.fieldBorder());
        pairingLinkArea.setText("Generating an ephemeral certificate and one-time token…");
        Ui.accessible(
                pairingLinkArea, "One-time pairing link", "Copy this link only to the person you want to approve.");
        card.add(pairingLinkArea);
        card.add(Box.createVerticalStrut(14));

        JPanel linkActions = new JPanel(new BorderLayout());
        linkActions.setOpaque(false);
        copyButton.setEnabled(false);
        linkActions.add(copyButton, BorderLayout.WEST);
        JPanel code = new JPanel();
        code.setOpaque(false);
        code.setLayout(new BoxLayout(code, BoxLayout.Y_AXIS));
        JLabel codeLabel = Ui.muted("VERIFY ON BOTH DEVICES");
        codeLabel.setFont(codeLabel.getFont().deriveFont(10f));
        verificationCodeLabel.setForeground(Theme.TEXT);
        verificationCodeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        code.add(codeLabel);
        code.add(Box.createVerticalStrut(4));
        code.add(verificationCodeLabel);
        linkActions.add(code, BorderLayout.EAST);
        card.add(linkActions);
        card.add(Box.createVerticalGlue());

        JPanel safety = new JPanel(new BorderLayout());
        safety.setBackground(Theme.ACCENT_SOFT);
        safety.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel safetyText = new JLabel("<html><b>View-only by default.</b> Mouse and keyboard input are ignored "
                + "until you explicitly enable control below.</html>");
        safetyText.setForeground(Theme.TEXT);
        safety.add(safetyText, BorderLayout.CENTER);
        card.add(Box.createVerticalStrut(22));
        card.add(safety);
        return card;
    }

    private JPanel buildActivityCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 14));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Ui.heading("Visible activity", 21));
        heading.add(Box.createVerticalStrut(6));
        heading.add(Ui.muted("Security events are also written to your local audit log."));
        card.add(heading, BorderLayout.NORTH);

        activityArea.setEditable(false);
        activityArea.setLineWrap(true);
        activityArea.setWrapStyleWord(true);
        activityArea.setBackground(Theme.SURFACE_RAISED);
        activityArea.setForeground(Theme.MUTED);
        activityArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        activityArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(activityArea);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void configureActions() {
        copyButton.addActionListener(event -> {
            String value = pairingLinkArea.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
            appendActivity("One-time link copied.");
            copyButton.setText("Copied");
            javax.swing.Timer timer = new javax.swing.Timer(1_500, ignored -> copyButton.setText("Copy one-time link"));
            timer.setRepeats(false);
            timer.start();
        });
        controlToggle.addActionListener(event -> {
            HostSessionServer current = server;
            if (current != null) {
                current.setControlEnabled(controlToggle.isSelected());
            }
        });
        disconnectButton.addActionListener(event -> {
            HostSessionServer current = server;
            if (current != null) {
                current.disconnectViewer();
            }
        });
        stopButton.addActionListener(event -> stopAndReturn());
    }

    private void startHostAsync() {
        if (closing) {
            return;
        }
        appendActivity("Creating an ephemeral TLS identity…");
        SwingWorker<StartedHost, Void> worker = new SwingWorker<>() {
            @Override
            protected StartedHost doInBackground() throws Exception {
                if (isCancelled() || closing) {
                    throw new CancellationException("Host startup was cancelled");
                }
                AwtScreenSource screenSource = new AwtScreenSource();
                HostSessionServer host = new HostSessionServer(
                        HostConfiguration.defaults(port, advertisedHost),
                        screenSource,
                        new AwtRemoteInputController(screenSource.bounds()),
                        HostFrame.this::requestApproval,
                        hostEvents(),
                        new JsonLineAuditLog(
                                Path.of(System.getProperty("user.home"), ".jdoor-assist", "audit"), Clock.systemUTC()));
                server = host;
                try {
                    if (isCancelled() || closing) {
                        throw new CancellationException("Host startup was cancelled");
                    }
                    PairingLink link = host.start();
                    if (isCancelled() || closing) {
                        throw new CancellationException("Host startup was cancelled");
                    }
                    return new StartedHost(host, link);
                } catch (Exception | Error failure) {
                    host.close();
                    clearServer(host);
                    throw failure;
                }
            }

            @Override
            protected void done() {
                if (startupWorker == this) {
                    startupWorker = null;
                }
                if (closing || !isDisplayable()) {
                    closePublishedServer();
                    return;
                }
                try {
                    StartedHost started = get();
                    if (closing || !isDisplayable()) {
                        closeServer(started.server());
                        return;
                    }
                    showPairingLink(started.link());
                    statusLabel.setText("Ready • view-only");
                    statusLabel.setForeground(Theme.SUCCESS);
                    appendActivity("Ready for one approved viewer.");
                } catch (CancellationException cancelled) {
                    closePublishedServer();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    closePublishedServer();
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause() == null ? failed : failed.getCause();
                    showStartFailure(cause);
                }
            }
        };
        startupWorker = worker;
        worker.execute();
    }

    private HostEventListener hostEvents() {
        return new HostEventListener() {
            @Override
            public void onPairingLinkChanged(PairingLink link) {
                onLiveEdt(() -> showPairingLink(link));
            }

            @Override
            public void onViewerConnected(String displayName, String remoteAddress) {
                onLiveEdt(() -> {
                    statusLabel.setText("Connected • view-only");
                    statusLabel.setForeground(Theme.SUCCESS);
                    copyButton.setEnabled(false);
                    controlToggle.setEnabled(true);
                    disconnectButton.setEnabled(true);
                    appendActivity(displayName + " connected from " + remoteAddress + ".");
                });
            }

            @Override
            public void onViewerDisconnected(String reason) {
                onLiveEdt(() -> {
                    statusLabel.setText("Ready • view-only");
                    statusLabel.setForeground(Theme.SUCCESS);
                    controlToggle.setSelected(false);
                    controlToggle.setEnabled(false);
                    disconnectButton.setEnabled(false);
                    appendActivity(reason + ". A new one-time link was generated.");
                });
            }

            @Override
            public void onControlChanged(boolean enabled) {
                onLiveEdt(() -> {
                    controlToggle.setSelected(enabled);
                    statusLabel.setText(enabled ? "Connected • control allowed" : "Connected • view-only");
                    statusLabel.setForeground(enabled ? Theme.WARNING : Theme.SUCCESS);
                    appendActivity(enabled ? "Remote mouse and keyboard control enabled." : "Remote control disabled.");
                });
            }

            @Override
            public void onActivity(String message) {
                onLiveEdt(() -> appendActivity(message));
            }

            @Override
            public void onError(String message, Throwable cause) {
                onLiveEdt(() -> appendActivity(message + ": " + safeMessage(cause)));
            }
        };
    }

    private boolean requestApproval(ConnectionRequest request) {
        if (closing) {
            return false;
        }
        AtomicBoolean approved = new AtomicBoolean();
        Runnable prompt = () -> {
            if (closing || !isDisplayable()) {
                return;
            }
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(Ui.heading(request.displayName() + " wants to view your screen", 18));
            content.add(Box.createVerticalStrut(12));
            content.add(new JLabel("Network address: " + request.remoteAddress().getHostAddress()));
            content.add(new JLabel("Verification code: " + request.verificationCode()));
            content.add(Box.createVerticalStrut(12));
            content.add(new JLabel("<html>Approve only if you initiated this support session and "
                    + "the code matches on the other device.<br><b>Approval "
                    + "shares your screen but does not enable control.</b></html>"));
            int result = JOptionPane.showConfirmDialog(
                    HostFrame.this, content, "Approve viewer?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            approved.set(result == JOptionPane.YES_OPTION);
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                prompt.run();
            } else {
                SwingUtilities.invokeAndWait(prompt);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException failed) {
            return false;
        }
        return approved.get();
    }

    private void showPairingLink(PairingLink link) {
        pairingLinkArea.setText(link.toString());
        pairingLinkArea.setCaretPosition(0);
        verificationCodeLabel.setText(link.verificationCode());
        copyButton.setEnabled(server == null || !server.hasViewer());
    }

    private void appendActivity(String message) {
        BoundedActivityLog.append(
                activityArea, "[" + LocalTime.now().format(ACTIVITY_TIME) + "] " + message.strip() + "\n");
        activityArea.setCaretPosition(activityArea.getDocument().getLength());
    }

    private void stopAndReturn() {
        stopSession(true);
    }

    private void stopAndExit() {
        stopSession(false);
    }

    private void stopSession(boolean returnToLauncher) {
        if (closing) {
            return;
        }
        closing = true;
        SwingWorker<StartedHost, Void> worker = startupWorker;
        if (worker != null) {
            worker.cancel(true);
        }
        HostSessionServer current = server;
        server = null;
        if (current != null) {
            current.close();
        }
        dispose();
        if (returnToLauncher) {
            new LauncherFrame().setVisible(true);
        }
    }

    private void showStartFailure(Throwable cause) {
        closePublishedServer();
        if (closing || !isDisplayable()) {
            return;
        }
        statusLabel.setText("Could not start");
        statusLabel.setForeground(Theme.ACCENT);
        appendActivity("Start failed: " + safeMessage(cause));
        JOptionPane.showMessageDialog(
                HostFrame.this,
                "JDoor Assist could not start screen sharing.\n\n"
                        + safeMessage(cause)
                        + "\n\nCheck screen-recording permissions and whether port "
                        + port
                        + " is available.",
                "Host session unavailable",
                JOptionPane.ERROR_MESSAGE);
    }

    private void closePublishedServer() {
        HostSessionServer current = server;
        server = null;
        if (current != null) {
            current.close();
        }
    }

    private void closeServer(HostSessionServer expected) {
        expected.close();
        clearServer(expected);
    }

    private void clearServer(HostSessionServer expected) {
        if (server == expected) {
            server = null;
        }
    }

    private void onLiveEdt(Runnable action) {
        onEdt(() -> {
            if (!closing && isDisplayable()) {
                action.run();
            }
        });
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private record StartedHost(HostSessionServer server, PairingLink link) {}
}
