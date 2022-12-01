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

public class ClientWebcamView extends Thread{
    private DatagramSocket socketWebcamView;
    private StreamView streamWebcamView;
    private ClientCommander commander;

    public ClientWebcamView(int port, ClientCommander commander) throws SocketException {
        socketWebcamView = new DatagramSocket(port);
        socketWebcamView.setSoTimeout(RESPONSE_TIMEOUT);
        this.commander = commander;
    }

    public DatagramSocket getSocketWebcamView() {
        return socketWebcamView;
    }

    public void setScreenView(StreamView streamView) {
        this.streamWebcamView = streamView;
    }

    private boolean isImageEnded(byte[] data) {
        if (data == null)
            return false;

        return data[0] == 'E' && data[1] == 'N' && data[2] == 'D';
    }

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

            } catch(SocketTimeoutException e) {
                commander.doCloseFromFrame();
            } catch (IOException e) {
                streamWebcamView.getGraphics().drawString("STREAM PROBLEMS", 0, 0);
            }
        }
        System.out.println("chiusura\n");
        socketWebcamView.close();
    }
}
