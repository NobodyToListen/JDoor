package com.jdoor.client;

import com.jdoor.Constants;
import com.jdoor.client.view.ScreenView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public final class ClientStreamView extends Thread{
    private final DatagramSocket socketView;
    private int screenHeight, screenWidth;
    private ScreenView screenView;
    private final ClientCommander commander;

    public ClientStreamView(int port, ClientCommander commander) throws SocketException {
        socketView = new DatagramSocket(port);
        socketView.setSoTimeout(30000);
        screenHeight = 0;
        screenWidth = 0;
        this.commander = commander;
    }

    public void setScreenDimension(String screenDimension) throws NumberFormatException{
        String[] dim = screenDimension.split("x");
        this.screenWidth = Integer.parseInt(dim[0]);
        this.screenHeight = Integer.parseInt(dim[1]);
        System.out.println(screenDimension);
    }
    public void setScreenDimension(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenView(ScreenView screenView) {
        this.screenView = screenView;
    }

    private boolean isImageEnded(byte[] data) {
        if (data == null)
            return false;

        return data[0] == 'E' && data[1] == 'N' && data[2] == 'D';
    }

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
                screenView.setScreen(finalImage.toByteArray());
                screenView.repaint();
                //System.out.println("Schermo ricevuto e disegnato con successo\n");
            } catch(SocketTimeoutException e) {
                commander.doCloseFromFrame();
            } catch (IOException e) {
                // ignore
            }
        }
        System.out.println("chiusura\n");
        socketView.close();
    }
}
