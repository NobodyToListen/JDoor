package com.jdoor.server;

import java.net.Socket;

public class ServerThread extends Thread {
    private Socket clientSocket;

    public ServerThread(Socket socket) {
        clientSocket = socket;
    }

    @Override
    public void run() {

    }
}
