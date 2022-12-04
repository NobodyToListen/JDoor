package com.jdoor.server;

import com.jdoor.server.screen.ScreenCaptureThread;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe del server.
 */
public class Server {

    public static void main(String[] args) throws IOException {
        // Avviare il socket del server.
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
            // Accettare una connessione.
            Socket client = ss.accept();

            System.out.println("Connection from " + client);

            // Avviare il thread del server per gestire il client.
            ServerThread st = new ServerThread(client);
            st.start();
            // Aggiungere client.
            screenCaptureThread.addClient(st);
        }
        screenCaptureThread.stopRunning();

        ss.close();
    }
}
