package com.jdoor.server;

import com.jdoor.server.screen.ScreenCaptureThread;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8080);
        ScreenCaptureThread screenCaptureThread;
        // Ottenere thread che manda le schermate.
        try {
            screenCaptureThread = ScreenCaptureThread.getScreenCaptureThread();
            screenCaptureThread.start();
        } catch (AWTException awte) {
            awte.printStackTrace();
            return;
        }

        boolean running = true;
        while (running) {
            Socket client = ss.accept();

            System.out.println("Connection from " + client);

            ServerThread st = new ServerThread(client);
            st.start();
            // Aggiungere classe.
            screenCaptureThread.addClient(st);
        }
        screenCaptureThread.stopRunning();

        ss.close();
    }
}
