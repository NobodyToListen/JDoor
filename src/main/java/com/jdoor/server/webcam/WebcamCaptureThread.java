package com.jdoor.server.webcam;

import com.github.sarxos.webcam.Webcam;
import com.jdoor.server.ServerThread;
import com.jdoor.server.screen.ScreenCaptureThread;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.jdoor.Constants.MINIMUM_WEBCAM_RESOLUTION;
import static com.jdoor.Constants.UDP_WEBCAM_PORT;

public class WebcamCaptureThread extends Thread{
    private static WebcamCaptureThread currentInstance;

    private final Webcam webcam;

    private final ArrayList<ServerThread> threads;
    private boolean running;

    private WebcamCaptureThread(){
        webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(MINIMUM_WEBCAM_RESOLUTION[0], MINIMUM_WEBCAM_RESOLUTION[1]));
        threads = new ArrayList<>();
        running = true;
    }

    public static synchronized WebcamCaptureThread getWebcamCaptureThread(){
        if (currentInstance == null)
            currentInstance = new WebcamCaptureThread();

        return currentInstance;
    }

    // Aggiungere un client alla lista di client a cui mandare lo schermo.
    public void addClient(ServerThread serverThread) {
        threads.add(serverThread);
    }

    public Webcam getWebcam() {
        return webcam;
    }

    // Metodo provato per ottenere una schermata.
    private byte[] getScreen() {
        BufferedImage image = webcam.getImage();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", byteArrayOutputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (IllegalArgumentException iae) {
            System.out.println("Impossibile accedere alla videocamera.");
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Metodo per fermare il thread.
    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        byte[] capture;
        while (running) {
            // Non ha senso eseguire il codice se non ci sono client collegati.
            if (threads.size() > 0 && webcam.isOpen()) {
                //System.out.println("OK");
                // Ottenere schermata.
                capture = getScreen();

                // Rimuovere i client morti.
                threads.removeIf(ServerThread::isClosed);

                // Mandare la schermata.
                for (ServerThread thread : threads) {
                    if(thread.isWatching()) {
                        thread.sendStream(capture, thread.getDatagramWebcamSocket(),  UDP_WEBCAM_PORT);
                    }
                }
            }

            // Aspettare 200 millisecondi.
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
