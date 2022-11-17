package com.jdoor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static boolean running = true;
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8080);

        while (running) {
            Socket client = ss.accept();

            ServerThread st = new ServerThread(client);
            st.start();
        }

        ss.close();
    }
}
