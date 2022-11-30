package com.jdoor.client;

import com.jdoor.FileOperationThread;
import com.jdoor.client.view.ClientFrame;
import com.jdoor.client.view.ScreenView;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ClientCommander extends Thread {
    private Socket socketCommands;
    private ClientStreamView streamView;
    private final BufferedWriter commandsWriter;
    private final BufferedReader resultReader;
    private final FileOperationThread fileOperationThread;
    private final ClientFrame cFrame;

    public ClientCommander(String ipAddress, int portTCP, int portUDP, ClientFrame cFrame) throws IOException {
        socketCommands = new Socket(InetAddress.getByName(ipAddress), portTCP);
        commandsWriter = new BufferedWriter(new OutputStreamWriter(socketCommands.getOutputStream()));
        resultReader = new BufferedReader(new InputStreamReader(socketCommands.getInputStream()));
        fileOperationThread = new FileOperationThread(new DataOutputStream(socketCommands.getOutputStream()), new DataInputStream(socketCommands.getInputStream()));
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
        if(command.charAt(0) == 'F' && (command.charAt(1) == 'R' || command.charAt(1) == 'S')) {
            String[] fileRequest = command.split(" ");
            switch (command.charAt(1)) {
                case 'R':
                    fileOperationThread.setFileToTransfer(new File(fileRequest[1]));
                    fileOperationThread.setOperation(FileOperationThread.Operations.Get);
                    break;
                case 'S':
                    fileOperationThread.setFileToTransfer(new File(fileRequest[2]));
                    fileOperationThread.setOperation(FileOperationThread.Operations.Send);
                    break;
            }


        } else {
            commandsWriter.write("C" + command + "\n");
            commandsWriter.flush();
        }
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
        String response = "";
        while(socketCommands != null) {
            try {
                if(fileOperationThread.isTransferring()) {
                    response = resultReader.readLine();
                }
            }catch (Exception e) {
                cFrame.getOutputArea().setText(e.getMessage());
            }
                if (streamView.getScreenHeight() == 0 && streamView.getScreenWidth() == 0) {
                    try {
                        sendScreenRequest();
                        streamView.setScreenView((ScreenView) cFrame.getScreenPanel());
                        streamView.setScreenDimension(response);
                        streamView.start();
                        fileOperationThread.start();
                    } catch (IOException e) {
                        streamView.setScreenDimension(0,0);
                    }
                } else {
                    cFrame.getOutputArea().append(response + "\n");
                }

        }
    }
}
