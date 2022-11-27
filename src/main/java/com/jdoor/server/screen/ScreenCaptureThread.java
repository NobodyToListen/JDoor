package com.jdoor.server.screen;

import com.jdoor.server.ServerThread;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.Deflater;

import static com.jdoor.server.Server.IMAGE_UDP_SIZE;

/**
 * Thread per mandare lo screen delo schermo.
 * Ne esiste uno solo che viene creato all'inizio del programma.
 */
public class ScreenCaptureThread extends Thread {
    private static ScreenCaptureThread currentInstance;

    public static String SCREEN_SIZE;

    private final Robot robot;
    private final Rectangle screenRectangle;

    private final ArrayList<ServerThread> threads;

    private boolean running;

    private ScreenCaptureThread() throws AWTException {
        robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenRectangle = new Rectangle(screenSize);

        threads = new ArrayList<>();

        SCREEN_SIZE = screenSize.width + "x" + screenSize.height;

        running = true;
    }

    public static synchronized ScreenCaptureThread getScreenCaptureThread() throws AWTException {
        if (currentInstance == null)
            currentInstance = new ScreenCaptureThread();

        return currentInstance;
    }

    // Aggiungere un client alla lista di client a cui mandare lo schermo.
    public void addClient(ServerThread serverThread) {
        threads.add(serverThread);
    }

    // Metodo provato per ottenere una schermata.
    private byte[] getScreen() {
        BufferedImage image = robot.createScreenCapture(screenRectangle);

        byte[] buffer;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", byteArrayOutputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        buffer = byteArrayOutputStream.toByteArray();

        Deflater deflater = new Deflater();
        deflater.setInput(buffer);
        deflater.finish();

        byte[] output = new byte[IMAGE_UDP_SIZE];
        deflater.deflate(output);
        deflater.end();

        return output;
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
                System.out.println("OK");
                // Ottenere schermata.
                capture = getScreen();

                // Rimuovere i client morti.
                threads.removeIf(ServerThread::isClosed);

                // Mandare la schermata.
                for (ServerThread thread : threads) {
                    System.out.println("SENT");
                    thread.sendScreen(capture);
                }
            }

            // Aspettare 200 millisecondi.
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
