package com.jdoor.server;

import com.jdoor.server.screen.ScreenCaptureThread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket clientSocket;
    private DatagramSocket datagramSocket;
    private InetAddress clientAddress;

    private BufferedReader clientInput;
    private BufferedWriter clientOutput;

    public ServerThread(Socket socket) throws IOException {
        clientSocket = socket;

        clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        clientOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        datagramSocket = new DatagramSocket();
        clientAddress = clientSocket.getInetAddress();
    }

    // Metodo per mandare la schermata.
    // Verrà chiamato solo da ScreenCaptureThread, NON VA CHIAMATO da nessun'altra parte.
    public void sendScreen(byte[] buffer) {
        // Creare pacchetto UDP e mandarlo.
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, clientAddress, 8081);
        try {
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Vedere se il thread è finito.
    public boolean isClosed() {
        return clientSocket == null;
    }

    @Override
    public void run() {
        try {
            clientOutput.write(ScreenCaptureThread.SCREEN_SIZE);
            clientOutput.flush();
            clientSocket.close();
            clientSocket = null; // Indica che il client socket è stato chiuso.
        } catch (IOException e) {
            System.out.println("Error while closing socket: " + e.getMessage());
        }
    }
}
