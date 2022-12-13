package com.jdoor.server.screen;

import com.jdoor.server.ServerThread;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import static com.jdoor.Constants.UDP_SCREEN_PORT;

/**
 * Thread per mandare lo screen dello schermo.
 * Ne esiste uno solo che viene creato all'inizio del programma.
 */
public class ScreenCaptureThread extends Thread {
    private static ScreenCaptureThread currentInstance;

    public static String SCREEN_SIZE;

    private final Robot robot;
    private final Rectangle screenRectangle;

    private final ArrayList<ServerThread> threads;
    private boolean running;

    /**
     * Costruttore della classe.
     * @throws AWTException Nel caso non si riesca ad accedere allo schermo (ad esempio in ambiente headless, vedi: <a href="https://stackoverflow.com/questions/13487025/headless-environment-error-in-java-awt-robot-class-with-mac-os">...</a>).
     */
    private ScreenCaptureThread() throws AWTException {
        robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenRectangle = new Rectangle(screenSize);

        threads = new ArrayList<>();

        SCREEN_SIZE = screenSize.width + "x" + screenSize.height;

        running = true;
    }

    /**
     * Metodo per ottenere l'istanza principale della classe.
     * @return L'istanza corrente della classe.
     * @throws AWTException Nel caso non ri riesca ad accedere allo schermo (vedi costruttore).
     */
    public static synchronized ScreenCaptureThread getScreenCaptureThread() throws AWTException {
        if (currentInstance == null)
            currentInstance = new ScreenCaptureThread();

        return currentInstance;
    }

    /**
     * Metodo per aggiungere un client alla lista di client a cui mandare lo schermo.
     * @param serverThread il Thread a cui mandare lo schermo.
     */
    public void addClient(ServerThread serverThread) {
        threads.add(serverThread);
    }

    /**
     * Metodo per ottenere una schermata del server.
     * @return Array di byte che rappresenta l'immagine in formato JPG.
     */
    private byte[] getScreen() {
        // Ottenere l'immagine dello schermo col robot.
        BufferedImage image = robot.createScreenCapture(screenRectangle);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // Scrivere l'immagine in formato JPG nello stream di bytes.
            ImageIO.write(image, "jpg", byteArrayOutputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Metodo per fermare il thread.
     */
    public void stopRunning() {
        running = false;
    }

    /**
     * Metodo principale del thread dove vengono mandate le schermate ai client.
     */
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
                        thread.sendStream(capture, thread.getDatagramScreenSocket(), UDP_SCREEN_PORT);
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
