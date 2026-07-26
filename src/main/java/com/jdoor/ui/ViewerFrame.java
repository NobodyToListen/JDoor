package com.jdoor.ui;

import com.jdoor.AppInfo;
import com.jdoor.protocol.WireMessage;
import com.jdoor.security.PairingLink;
import com.jdoor.session.ViewerClient;
import com.jdoor.session.ViewerEventListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class ViewerFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String JOIN_CARD = "join";
    private static final String SESSION_CARD = "session";

    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);
    private final JTextArea pairingLinkArea = new JTextArea();
    private final JTextField displayNameField = new JTextField();
    private final JButton connectButton = Ui.button("Request access", true);
    private final JLabel joinStatusLabel = Ui.muted("The host will see your name and approve locally.");
    private final JPanel joinVerificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel joinVerificationLabel = Ui.plain("—");
    private final RemoteScreenPanel screenPanel = new RemoteScreenPanel();
    private final JLabel sessionStatusLabel = Ui.plain("Connecting…");
    private final JLabel permissionLabel = Ui.plain("VIEW ONLY");
    private final JLabel verificationLabel = Ui.plain("—");
    private final JButton disconnectButton = Ui.button("Disconnect", false);
    private final transient RemoteInputDispatcher inputDispatcher;
    private final AtomicLong lastPointerMove = new AtomicLong();

    private transient volatile ViewerClient client;
    private transient volatile SwingWorker<ConnectedViewer, Void> connectWorker;
    private volatile boolean closing;

    public ViewerFrame(String initialPairingLink) {
        super(AppInfo.NAME + " — Viewer");
        inputDispatcher = new RemoteInputDispatcher(this::releaseCurrentInputs, failure -> showInputFailure());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1_100, 760);
        setLocationRelativeTo(null);
        BrandIcon.apply(this);
        cardContainer.add(buildJoinPanel(), JOIN_CARD);
        cardContainer.add(buildSessionPanel(), SESSION_CARD);
        setContentPane(cardContainer);
        configureRemoteInput();
        configureActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeWindow(false);
            }
        });
        if (initialPairingLink != null) {
            pairingLinkArea.setText(initialPairingLink);
            SwingUtilities.invokeLater(this::connectAsync);
        }
    }

    private JPanel buildJoinPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(36, 52, 36, 52));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(Ui.muted("JOIN A TRUSTED SESSION"));
        header.add(Box.createVerticalStrut(8));
        header.add(Ui.heading("Paste the one-time link.", 30));
        header.add(Box.createVerticalStrut(8));
        header.add(Ui.muted("The embedded certificate fingerprint prevents a different host from "
                + "silently impersonating the person you intend to help."));
        root.add(header, BorderLayout.NORTH);

        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)));
        JLabel linkLabel = Ui.heading("Pairing link", 16);
        card.add(linkLabel);
        card.add(Box.createVerticalStrut(8));
        pairingLinkArea.setRows(5);
        pairingLinkArea.setLineWrap(true);
        pairingLinkArea.setWrapStyleWord(false);
        pairingLinkArea.setBackground(Theme.SURFACE_RAISED);
        pairingLinkArea.setForeground(Theme.TEXT);
        pairingLinkArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        pairingLinkArea.setBorder(Ui.fieldBorder());
        pairingLinkArea.setToolTipText("jdoor://…/join?token=…&fingerprint=…");
        Ui.accessible(pairingLinkArea, "Pairing link", "Paste the complete one-time link supplied by the host.");
        card.add(new JScrollPane(pairingLinkArea));
        card.add(Box.createVerticalStrut(18));
        card.add(Ui.heading("Your device name", 16));
        card.add(Box.createVerticalStrut(8));
        displayNameField.setText(defaultDisplayName());
        displayNameField.setBackground(Theme.SURFACE_RAISED);
        displayNameField.setForeground(Theme.TEXT);
        displayNameField.setBorder(Ui.fieldBorder());
        Ui.accessible(displayNameField, "Device name", "This name is shown to the host in the approval prompt.");
        card.add(displayNameField);
        card.add(Box.createVerticalStrut(16));
        joinVerificationPanel.setOpaque(false);
        JLabel verificationCaption = Ui.muted("VERIFY WITH HOST");
        verificationCaption.setFont(verificationCaption.getFont().deriveFont(Font.BOLD, 11f));
        joinVerificationLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        joinVerificationLabel.setForeground(Theme.TEXT);
        Ui.accessible(
                joinVerificationLabel,
                "Host verification code",
                "Compare this code with the code shown by the host before they approve you.");
        joinVerificationPanel.add(verificationCaption);
        joinVerificationPanel.add(joinVerificationLabel);
        joinVerificationPanel.setVisible(false);
        card.add(joinVerificationPanel);
        card.add(Box.createVerticalStrut(20));
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(connectButton, BorderLayout.WEST);
        actions.add(joinStatusLabel, BorderLayout.CENTER);
        joinStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        card.add(actions);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(30, 120, 40, 120));
        center.add(card, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JButton backButton = Ui.button("Back", false);
        backButton.addActionListener(event -> closeWindow(true));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(backButton);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildSessionPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        status.setOpaque(false);
        sessionStatusLabel.setForeground(Theme.SUCCESS);
        sessionStatusLabel.setFont(sessionStatusLabel.getFont().deriveFont(Font.BOLD));
        permissionLabel.setForeground(Theme.SUCCESS);
        permissionLabel.setFont(permissionLabel.getFont().deriveFont(Font.BOLD, 11f));
        permissionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true), BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        status.add(sessionStatusLabel);
        status.add(permissionLabel);
        header.add(status, BorderLayout.WEST);
        header.add(disconnectButton, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel screenCard = new JPanel(new BorderLayout());
        screenCard.setBackground(Theme.SURFACE);
        screenCard.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        screenCard.add(screenPanel, BorderLayout.CENTER);
        root.add(screenCard, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel hint = Ui.muted(
                "Click the shared screen to focus it. Input is sent only while the host " + "shows “control allowed”.");
        footer.add(hint, BorderLayout.WEST);
        JPanel verification = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        verification.setOpaque(false);
        verification.add(Ui.muted("VERIFY"));
        verificationLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        verificationLabel.setForeground(Theme.TEXT);
        verification.add(verificationLabel);
        footer.add(verification, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void configureActions() {
        connectButton.addActionListener(event -> connectAsync());
        disconnectButton.addActionListener(event -> {
            ViewerClient current = client;
            if (current != null) {
                current.disconnect();
            }
        });
    }

    private void configureRemoteInput() {
        screenPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                screenPanel.requestFocusInWindow();
                sendPointer(event, WireMessage.PointerAction.PRESS, event.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                sendPointer(event, WireMessage.PointerAction.RELEASE, event.getButton());
            }
        });
        screenPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                sendPointerMove(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                sendPointerMove(event);
            }
        });
        screenPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                sendKeyboard(new WireMessage.KeyboardInput(
                        WireMessage.KeyAction.PRESS, event.getKeyCode(), event.getModifiersEx() & 0xFFFF));
                event.consume();
            }

            @Override
            public void keyReleased(KeyEvent event) {
                sendKeyboard(new WireMessage.KeyboardInput(
                        WireMessage.KeyAction.RELEASE, event.getKeyCode(), event.getModifiersEx() & 0xFFFF));
                event.consume();
            }
        });
        screenPanel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                releaseAllRemoteInputs();
            }
        });
    }

    private void connectAsync() {
        if (closing || (connectWorker != null && !connectWorker.isDone())) {
            return;
        }
        String rawLink = pairingLinkArea.getText().strip();
        String displayName = displayNameField.getText().strip();
        PairingLink link;
        try {
            link = PairingLink.parse(rawLink);
        } catch (IllegalArgumentException invalidLink) {
            joinVerificationPanel.setVisible(false);
            joinStatusLabel.setText(safeMessage(invalidLink));
            return;
        }
        joinVerificationLabel.setText(link.verificationCode());
        joinVerificationPanel.setVisible(true);
        connectButton.setEnabled(false);
        connectButton.setText("Connecting securely…");
        joinStatusLabel.setText("Compare this code with the host while they approve locally…");
        SwingWorker<ConnectedViewer, Void> worker = new SwingWorker<>() {
            private volatile ViewerClient candidate;

            @Override
            protected ConnectedViewer doInBackground() throws Exception {
                if (isCancelled() || closing) {
                    throw new CancellationException("Connection was cancelled");
                }
                ViewerClient viewer = new ViewerClient(link, displayName, viewerEvents());
                candidate = viewer;
                client = viewer;
                if (isCancelled() || closing) {
                    viewer.close();
                    throw new CancellationException("Connection was cancelled");
                }
                WireMessage.ServerHello hello = viewer.connect();
                if (isCancelled() || closing) {
                    viewer.close();
                    throw new CancellationException("Connection was cancelled");
                }
                return new ConnectedViewer(viewer, link, hello);
            }

            @Override
            protected void done() {
                if (connectWorker == this) {
                    connectWorker = null;
                }
                if (closing || !isDisplayable()) {
                    closeClient(candidate);
                    return;
                }
                connectButton.setEnabled(true);
                connectButton.setText("Request access");
                try {
                    ConnectedViewer connected = get();
                    if (closing || !isDisplayable()) {
                        closeClient(connected.viewer());
                        return;
                    }
                    client = connected.viewer();
                    verificationLabel.setText(connected.link().verificationCode());
                    sessionStatusLabel.setText("Connected • "
                            + connected.hello().screenWidth()
                            + "×"
                            + connected.hello().screenHeight());
                    cards.show(cardContainer, SESSION_CARD);
                    screenPanel.requestFocusInWindow();
                } catch (CancellationException cancelled) {
                    closeClient(candidate);
                    joinStatusLabel.setText("Connection cancelled.");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    closeClient(candidate);
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause() == null ? failed : failed.getCause();
                    showConnectionFailure(candidate, cause);
                }
            }
        };
        connectWorker = worker;
        worker.execute();
    }

    private ViewerEventListener viewerEvents() {
        return new ViewerEventListener() {
            @Override
            public void onScreenFrame(WireMessage.ScreenFrame frame) {
                try {
                    screenPanel.setFrame(frame);
                } catch (IOException invalidFrame) {
                    onEdt(() -> sessionStatusLabel.setText("Connected • skipped an invalid frame"));
                }
            }

            @Override
            public void onControlChanged(boolean enabled) {
                onEdt(() -> {
                    permissionLabel.setText(enabled ? "CONTROL ALLOWED" : "VIEW ONLY");
                    permissionLabel.setForeground(enabled ? Theme.WARNING : Theme.SUCCESS);
                    if (enabled) {
                        screenPanel.requestFocusInWindow();
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {
                onEdt(() -> {
                    inputDispatcher.clearPending();
                    if (!closing) {
                        cards.show(cardContainer, JOIN_CARD);
                        joinStatusLabel.setText(reason);
                    }
                    client = null;
                });
            }

            @Override
            public void onError(String message, Throwable cause) {
                onEdt(() -> sessionStatusLabel.setText(message + ": " + safeMessage(cause)));
            }
        };
    }

    private void sendPointerMove(MouseEvent event) {
        long now = System.nanoTime();
        long previous = lastPointerMove.get();
        if (now - previous < 33_000_000L || !lastPointerMove.compareAndSet(previous, now)) {
            return;
        }
        sendPointer(event, WireMessage.PointerAction.MOVE, 0);
    }

    private void sendPointer(MouseEvent event, WireMessage.PointerAction action, int button) {
        ViewerClient current = client;
        if (current == null || !current.isControlEnabled()) {
            return;
        }
        (action == WireMessage.PointerAction.RELEASE
                        ? screenPanel.normalizedPointClamped(event.getX(), event.getY())
                        : screenPanel.normalizedPoint(event.getX(), event.getY()))
                .ifPresent(point -> {
                    WireMessage.PointerInput input = new WireMessage.PointerInput(action, point.x(), point.y(), button);
                    RemoteInputDispatcher.InputAction task = () -> current.sendPointer(input);
                    if (action == WireMessage.PointerAction.MOVE) {
                        inputDispatcher.submitMove(task);
                    } else {
                        inputDispatcher.submitDiscrete(task);
                    }
                });
    }

    private void sendKeyboard(WireMessage.KeyboardInput input) {
        ViewerClient current = client;
        if (current == null || !current.isControlEnabled()) {
            return;
        }
        inputDispatcher.submitDiscrete(() -> current.sendKeyboard(input));
    }

    private void releaseAllRemoteInputs() {
        inputDispatcher.submitReleaseAll(this::releaseCurrentInputs);
    }

    private void releaseCurrentInputs() throws IOException {
        ViewerClient current = client;
        if (current != null) {
            current.releaseAllInputs();
        }
    }

    private void showInputFailure() {
        onEdt(() -> {
            if (!closing && isDisplayable()) {
                sessionStatusLabel.setText("Input channel failed");
            }
        });
    }

    private void showConnectionFailure(ViewerClient failedClient, Throwable cause) {
        closeClient(failedClient);
        if (closing || !isDisplayable()) {
            return;
        }
        joinStatusLabel.setText(safeMessage(cause));
        JOptionPane.showMessageDialog(
                ViewerFrame.this,
                "The session could not be joined.\n\n" + safeMessage(cause),
                "Connection failed",
                JOptionPane.ERROR_MESSAGE);
    }

    private void closeClient(ViewerClient expected) {
        if (expected != null) {
            expected.close();
        }
        if (client == expected) {
            client = null;
        }
    }

    private void closeWindow(boolean returnToLauncher) {
        if (closing) {
            return;
        }
        closing = true;
        SwingWorker<ConnectedViewer, Void> worker = connectWorker;
        if (worker != null) {
            worker.cancel(true);
        }
        ViewerClient current = client;
        client = null;
        if (current != null) {
            current.close();
        }
        inputDispatcher.close();
        dispose();
        if (returnToLauncher) {
            new LauncherFrame().setVisible(true);
        }
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static String defaultDisplayName() {
        String candidate = System.getProperty("user.name", "Viewer")
                .replaceAll("\\p{Cntrl}", " ")
                .strip();
        if (candidate.isEmpty()) {
            return "Viewer";
        }
        return candidate.substring(0, Math.min(candidate.length(), 48)) + "'s device";
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private record ConnectedViewer(ViewerClient viewer, PairingLink link, WireMessage.ServerHello hello) {}
}
