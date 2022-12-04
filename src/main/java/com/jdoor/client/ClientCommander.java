package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;
import com.jdoor.client.view.ScreenView;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

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
    }

    public void sendMousePosition(int mouseX, int mouseY, char button) throws IOException {
        float scaledMouseXf = (float) streamView.getScreenWidth() / ((float) cFrame.getScreenPanel().getWidth() / mouseX);
        float scaledMouseYf = (float) streamView.getScreenHeight()/((float) cFrame.getScreenPanel().getHeight()/mouseY);
        //System.out.println(scaledMouseX);
        int scaledMouseX = Math.round(scaledMouseXf);
        int scaledMouseY = Math.round(scaledMouseYf);
        //System.out.println(streamView.getScreenHeight() + "/(" + cFrame.getScreenPanel().getWidth() + "/" + mouseX + ")= " + scaledMouseX);
        String command = "M" + button + String.valueOf(scaledMouseX) + ";" + String.valueOf(scaledMouseY) + "\n";
        System.out.println(command);
        commandsWriter.write(command);
        commandsWriter.flush();
    }
    public void sendCloseMessage() throws IOException {
        commandsWriter.write("S\n");
        commandsWriter.flush();
    }
    public void doCloseFromFrame() {
        if(cFrame.getDisconnectBtn().isEnabled()) {
            cFrame.getDisconnectBtn().doClick();
        }
    }

    public void sendKey(int keyCode) throws IOException {
        commandsWriter.write("K" + String.valueOf(keyCode) + "\n");
        commandsWriter.flush();
    }

    public void sendCommands(String command) throws IOException {
        commandsWriter.write("C" + command + "\n");
        commandsWriter.flush();
    }
    public void sendScreenStopStart() throws IOException {
        commandsWriter.write("L\n");
        commandsWriter.flush();
    }

    public void sendScreenRequest() throws IOException {
        commandsWriter.write("R\n");
        commandsWriter.flush();
    }

    public Socket getSocketCommands() {
        return socketCommands;
    }

    @Override
    public void run() {
        while(socketCommands != null) {
            if(streamView.getScreenHeight() == 0 && streamView.getScreenWidth() == 0) {
                try {
                    sendScreenRequest();
                    streamView.setScreenView((ScreenView) cFrame.getScreenPanel());
                    streamView.setScreenDimension(resultReader.readLine());
                    streamView.start();
                    System.out.println("Schermo ricevuto con successo\n");
                } catch (Exception e) {
                    cFrame.getOutputArea().setText("Error:" + e.getMessage() + "\n");
                    streamView.setScreenDimension(0);
                }
            } else {
                try {
                    cFrame.getOutputArea().setText(resultReader.readLine() + "\n");
                } catch (Exception e) {
                    cFrame.getOutputArea().setText("Error:" + e.getMessage() + "\n");
                }
            }
        }
    }
}
