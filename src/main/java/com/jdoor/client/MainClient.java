package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

import java.awt.event.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe principale del client.
 */
public class MainClient implements MouseListener, KeyListener, WindowListener, ActionListener {
    private final ClientFrame clientFrame;
    private ClientCommander commander;

    /**
     * Costruttore della classe.
     * @param clientFrame Il frame del client a cui aggiungere i listener.
     */
    public MainClient(ClientFrame clientFrame) {
        this.clientFrame = clientFrame;
        this.clientFrame.addWindowListener(this);
        this.clientFrame.getOperationBtn().addActionListener(this);
        this.clientFrame.getDisconnectBtn().addActionListener(this);
    }

    /**
     * Metodo per controllare se l'IP a cui ci si vuole connettere è valido.
     * @param ip L'IP a cui ci si vuole connettere.
     * @return true se l'Ip è valido, false se no.
     */
    private boolean isValidIP(String ip) {
        if (ip == null) {
            return false;
        }else if (ip.equals("localhost")) {
            return true;
        } else {
            // Pezzo di espressione regolare per vedere se il numero va da 0 a 255.
            String zeroTo255
                    = "(\\d{1,2}|(0|1)\\"
                    + "d{2}|2[0-4]\\d|25[0-5])";

            // Espressione regolare per l'IP completo.
            String regex
                    = zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255;

            // Compilare e verificare la regexp.
            Pattern p = Pattern.compile(regex);

            Matcher m = p.matcher(ip);
            return m.matches();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Bottone per connettersi e mandare comandi.
        if(e.getSource() == clientFrame.getOperationBtn()) {
            // Se si ha premuto il bottone per connettersi al server.
            if(clientFrame.getOperationBtn().getText().equals("CONNECT")) {
                // Prendere l'IP e validarlo.
                String ip = clientFrame.getInputField().getText();
                if(isValidIP(ip)) {
                    try {
                        // Creare novo ClientCommander e connettersi al server.
                        int TCP_PORT = 8080;
                        int UDP_PORT = 8081;
                        commander = new ClientCommander(ip, TCP_PORT, UDP_PORT,clientFrame);
                        // Aggiungere i listener agli altri componenti.
                        clientFrame.getScreenPanel().addMouseListener(this);
                        clientFrame.addKeyListener(this);
                        // Modificare la UI in modo che rispecchi la situazione attuale.
                        clientFrame.getOperationBtn().setText("SEND");
                        clientFrame.getDisconnectBtn().setEnabled(true);
                        clientFrame.getInputLabel().setText("CMD");
                        commander.start();
                    } catch (Exception ex) {
                        // Mostrare messaggio di errore nella schermata di output.
                        clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                    }
                } else {
                    // Mostrare messaggio di errore nella schermata di output.
                    clientFrame.getOutputArea().setText("ERROR: Invalid ip\n");
                }
            } else { // Se si ha premuto il bottone per mandare un comando.
                try {
                    // Prendere il comando dalla riga di testo e mandarlo al server.
                    commander.sendCommands(clientFrame.getInputField().getText());
                    System.out.println("COMMAND:" + clientFrame.getInputField().getText() + " sent" + "\n");
                } catch (Exception ex) {
                    // Mostrare messaggio di errore nella schermata di output.
                    clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                }
            }
        } else { // Bottone per chiudere la connessione.
            try {
                // Chiudere tutte le connessioni al server.
                commander.closeConnection();
                // Rimuovere i listener ai componenti.
                clientFrame.getScreenPanel().removeMouseListener(this);
                clientFrame.removeKeyListener(this);
                // Reimpostare la UI.
                clientFrame.getOperationBtn().setText("CONNECT");
                clientFrame.getDisconnectBtn().setEnabled(false);
                clientFrame.getInputLabel().setText("HOST");
            } catch (Exception ex) {
                // Mostrare errore se non ci si riesce a disconnettere.
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
        clientFrame.getOutputArea().setText("");
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Ottenere che tasto del mouse si ha premuto.
        char button = 'L';
        if(e.getButton() != MouseEvent.BUTTON1) {
            button = 'R';
        }

        // Mandare il comando con tasto e coordinate, mostrare errore nel caso.
        try {
            commander.sendMousePosition(e.getX(), e.getY(), button);
        } catch (IOException ex) {
            clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        clientFrame.setFocusable(true);
        clientFrame.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        clientFrame.setFocusable(false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            // Ottenere il codice del tasto e mandarlo, mostrare errore nel caso.
            commander.sendKey(e.getKeyCode());
            System.out.println("KEY:" + e.getKeyCode() + " sent" + "\n");
        } catch (Exception ex) {
            clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            // Se ci sono connessioni, allora le chiudiamo quando usciamo dal programma.
            // Mostriamo errore se neccesario.
            if(commander != null && commander.getSocketCommands() != null) {
                    try {
                        commander.closeConnection();
                    } catch (Exception ex) {
                        clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
                    }
            }
            System.exit(0);
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            // Quando riduciamo a icona la finestra diciamo al server che non stiamo più
            // guardando lo schermo così da non farci mandare dati e da ridurre la
            // connessione utilizzzata.
            try {
                commander.sendScreenStopStart();
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        if(e.getSource() == clientFrame) {
            // Se la finestra viene ringrandita allora diciamo al server
            // che abbiamo ripreso a visualizzare lo schermo.
            try {
                commander.sendScreenStopStart();
            } catch (Exception ex) {
                clientFrame.getOutputArea().setText("ERROR:" + ex.getMessage() + "\n");
            }
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    public static void main(String[] args) {
        ClientFrame cFrame = new ClientFrame();
        new MainClient(cFrame);
    }
}
