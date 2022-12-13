package com.jdoor.client;

import com.jdoor.Constants;
import com.jdoor.client.view.StreamView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static com.jdoor.Constants.RESPONSE_TIMEOUT;

/**
 * Thread per gestire lo stream dello schermo al client.
 */
public class ClientScreenView extends Thread {
    private final DatagramSocket socketView;
    private int screenHeight, screenWidth;
    private StreamView streamView;
    private final ClientCommander commander;
    private boolean watching;

    /**
     * Costruttore del thread.
     * @param port La porta su cui ricevere lo stream.
     * @param commander Il ClientCommander da usare.
     * @throws SocketException Se non si riuscisse a creare il socket UDP.
     */
    public ClientScreenView(int port, ClientCommander commander) throws SocketException {
        socketView = new DatagramSocket(port);
        socketView.setSoTimeout(RESPONSE_TIMEOUT);
        screenHeight = 0;
        screenWidth = 0;
        this.commander = commander;
    }

    /**
     * Metodo per impostare la grandezza dello schermo remoto.
     * @param screenDimension La stringa formattata per la dimensione dello schermo.
     * @throws NumberFormatException Nel caso i numeri non fossero formattati correttamente.
     */
    public void setScreenDimension(String screenDimension) throws NumberFormatException {
        String[] dim = screenDimension.split("x");
        this.screenWidth = Integer.parseInt(dim[0]);
        this.screenHeight = Integer.parseInt(dim[1]);
        System.out.println(screenDimension);
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenView(StreamView streamView) {
        this.streamView = streamView;
    }

    public boolean isWatching() {
        return watching;
    }

    public void setWatching(boolean watching) {
        this.watching = watching;
    }

    /**
     * Metodo per capire se il frammento UDP che si ha ricevuto indica la fine dell'immagine.
     * @param data Dati ottenuti dal socket UDP.
     * @return true se l'immagine ha finito di arrivare, false se no.
     */
    private boolean isImageEnded(byte[] data) {
        if (data == null)
            return false;

        return data[0] == 'E' && data[1] == 'N' && data[2] == 'D';
    }

    /**
     * Metodo principale del thread dove si riceve l'immagine e la si mostra a schermo.
     */
    @Override
    public void run() {
        while (commander.getSocketCommands() != null) {
            //System.out.println("Inizio ricezione schermo\n");
            try {
                // Creare il buffer per ottenere i dati dell'immagine.
                byte[] data = new byte[Constants.IMAGE_BYTES_DIMENSION];
                ByteArrayOutputStream finalImage = new ByteArrayOutputStream();

                // Fino a che non si ha finito di leggere l'immagine, continuiamo a ricevere i pacchetti
                // UDP e a concatenarli per ottenere l'immagine completa.
                while (!isImageEnded(data)) {
                    DatagramPacket pkt = new DatagramPacket(data, Constants.IMAGE_BYTES_DIMENSION);
                    socketView.receive(pkt);
                    finalImage.write(pkt.getData());
                }
                // Impostare la nuova immagine visualizzata.
                streamView.setScreen(finalImage.toByteArray());
                //System.out.println("Schermo ricevuto e disegnato con successo\n");
            } catch(SocketTimeoutException e) {
                if(watching) {
                    commander.doCloseFromFrame();
                }
            } catch (IOException e) {
                streamView.getGraphics().drawString("STREAM PROBLEMS", 0, 0);
            }
        }
        System.out.println("chiusura\n");
        socketView.close();
    }
}
