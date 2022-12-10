package com.jdoor.client;

import com.jdoor.Constants;
import com.jdoor.client.view.ClientFrame;
import com.jdoor.client.view.StreamView;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import static com.jdoor.Constants.*;

public class ClientCommander extends Thread {
    private Socket socketCommands;
    private ClientScreenView screenView;
    private ClientWebcamView webcamView;
    private final BufferedWriter commandsWriter;
    private final BufferedReader resultReader;
    private final ClientFrame cFrame;

    public ClientCommander(String ipAddress, ClientFrame cFrame) throws IOException {
        socketCommands = new Socket(InetAddress.getByName(ipAddress), TCP_PORT);


        commandsWriter = new BufferedWriter(new OutputStreamWriter(socketCommands.getOutputStream()));
        resultReader = new BufferedReader(new InputStreamReader(socketCommands.getInputStream()));


        screenView = new ClientScreenView(UDP_SCREEN_PORT, this);
        webcamView = new ClientWebcamView(UDP_WEBCAM_PORT, this);

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
        float scaledMouseXf = (float) screenView.getScreenWidth() / ((float) cFrame.getScreenPanel().getWidth() / mouseX);
        float scaledMouseYf = (float) screenView.getScreenHeight()/((float) cFrame.getScreenPanel().getHeight()/mouseY);
        int scaledMouseX = Math.round(scaledMouseXf);
        int scaledMouseY = Math.round(scaledMouseYf);
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
        if(cFrame.getDiconnectBtn().isEnabled()) {
            cFrame.getDiconnectBtn().doClick();
        }
    }

    public void sendKey(int keyCode) throws IOException {
        commandsWriter.write("K" + String.valueOf(keyCode) + "\n");
        commandsWriter.flush();
    }

    public void sendCommands(String command) throws IOException {
        if(command.equals("WO") || command.equals("WC")) {
            commandsWriter.write(command + "\n");
        } else {
            commandsWriter.write("C" + command + "\n");
        }
        commandsWriter.flush();
    }
    public void sendScreenStopStart() throws IOException {
        if(screenView.isWatching()) {
            screenView.setWatching(false);
        } else {
            screenView.setWatching(true);
        }
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
                    cFrame.getOutputArea().setText(resultReader.readLine() + "\n");
                }
            }catch (IOException e) {
                cFrame.getOutputArea().setText(e.getMessage());
                socketCommands = null;
                doCloseFromFrame();
            }
        }
    }
}
