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

import static com.jdoor.Constants.WEBCAM_CAPTURE_SIZE;

public class WebcamCaptureThread extends Thread{
    private static WebcamCaptureThread currentInstance;

    private final Webcam webcam;

    private final ArrayList<ServerThread> threads;
    private boolean running;

    private WebcamCaptureThread(){
        webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(176, 144));
        webcam.open();
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

    // Metodo provato per ottenere una schermata.
    private byte[] getScreen() {
        BufferedImage image = webcam.getImage();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", byteArrayOutputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
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
            if (threads.size() > 0) {
                //System.out.println("OK");
                // Ottenere schermata.
                capture = getScreen();

                // Rimuovere i client morti.
                threads.removeIf(ServerThread::isClosed);

                // Mandare la schermata.
                for (ServerThread thread : threads) {
                    if(thread.isWatching()) {
                        thread.sendWebcam(capture);
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
