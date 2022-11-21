package com.jdoor.server;

import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread {
    private final Socket clientSocket;

    private BufferedReader clientInput;
    private BufferedWriter clientOutput;

    public ServerThread(Socket socket) throws IOException {
        clientSocket = socket;

        clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        clientOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void run() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing socket: " + e.getMessage());
        }
    }
}
