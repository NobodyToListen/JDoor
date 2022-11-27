package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;
import com.jdoor.client.view.ScreenView;

import javax.swing.*;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class ClientCommander extends Thread {
    private Socket socketCommands;
    private BufferedWriter commandsWriter;
    private BufferedReader resultReader;
    private ClientStreamView streamView;
    private ClientFrame cFrame;

    public ClientCommander(String ipAddress, int portTCP, int portUDP, ClientFrame cFrame) throws IOException {
        socketCommands = new Socket(InetAddress.getByName(ipAddress), portTCP);
        commandsWriter = new BufferedWriter(new OutputStreamWriter(socketCommands.getOutputStream()));
        resultReader = new BufferedReader(new InputStreamReader(socketCommands.getInputStream()));
        streamView = new ClientStreamView(portUDP, this);
        this.cFrame = cFrame;
    }

    public void closeConnection() throws IOException {
        sendCloseMessage();
        commandsWriter.close();
        resultReader.close();
        socketCommands.close();
        socketCommands = null;
        this.interrupt();
    }

    public void sendMousePosition(int mouseX, int mouseY, char button) throws IOException {
        int scaledMouseX = streamView.getScreenWidth()/(cFrame.getScreenPanel().getWidth()/mouseX);
        int scaledMouseY = streamView.getScreenHeight()/(cFrame.getScreenPanel().getHeight()/mouseY);
        commandsWriter.write("M" + button + String.valueOf(scaledMouseX) + ";" + String.valueOf(scaledMouseY) + "\n");
    }
    public void sendCloseMessage() throws IOException {
        commandsWriter.write("S");
    }
    public void doCloseFromFrame() {
        if(cFrame.getDiconnectBtn().isEnabled()) {
            cFrame.getDiconnectBtn().doClick();
        }
    }

    public void sendKey(int keyCode) throws IOException {
        commandsWriter.write("K" + String.valueOf(keyCode));
    }

    public void sendCommands(String command) throws IOException {
        commandsWriter.write("C" + command + "\n");
    }

    public Socket getSocketCommands() {
        return socketCommands;
    }

    @Override
    public void run() {
        while(socketCommands != null) {
            if(streamView.getScreenHeight() == 0 && streamView.getScreenWidth() == 0) {
                try {
                    commandsWriter.write("R\n");
                    commandsWriter.flush();
                    streamView.setScreenView((ScreenView) cFrame.getScreenPanel());
                    streamView.setScreenDimension(resultReader.readLine());
                    streamView.start();
                    System.out.println("Schermo ricevuto con successo\n");
                } catch (Exception e) {
                    streamView.setScreenDimension(0,0);
                    System.out.println("problemi nella ricezione delle dimensioni dello schermo\n");
                }
            } else {
                try {
                    cFrame.getOutputArea().append(resultReader.readLine() + "\n");
                } catch (IOException e) {
                    System.out.println("Error:" + e.getMessage() + "\n");
                }
            }
        }
    }
}
