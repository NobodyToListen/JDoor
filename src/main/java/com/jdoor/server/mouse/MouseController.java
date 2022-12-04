package com.jdoor.server.mouse;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Arrays;

/**
 * Classe per controllare il mouse.
 * C'è nè solo una istanza per tutta la durata del programma.
 */
public class MouseController {
    private static MouseController currentInstance;
    private final Robot robot;

    /**
     * Costruttore della classe.
     * @throws AWTException Nel caso non si riesca ad accedere allo schermo (ad esempio in ambiente headless, vedi: <a href="https://stackoverflow.com/questions/13487025/headless-environment-error-in-java-awt-robot-class-with-mac-os">...</a>).
     */
    private MouseController() throws AWTException {
        robot = new Robot();
    }

    /**
     * Metodo per ottenere l'istanza principale della classe.
     * @return L'istanza corrente della classe.
     * @throws AWTException Nel caso non ri riesca ad accedere allo schermo (vedi costruttore).
     */
    public static synchronized MouseController getInstance() throws AWTException {
        if (currentInstance == null)
            currentInstance = new MouseController();
        return currentInstance;
    }

    /**
     * Metodo per cliccare il mouse sul server.
     * Questo metodo può essere chiamato da un solo thread per volta.
     * Esempio comando: MR156;56.
     * @param command Il comando da eseguire.
     */
    public synchronized void clickMouse(String command) {
        // Ottenere la direzione in cui si sta cliccando il mouse.
        char direction = command.charAt(1);

        // Ottenere le coordinate di dove va mosso il mouse.
        String rawCords = command.substring(2);
        String[] cords = rawCords.split(";");

        System.out.println(rawCords);
        System.out.println(Arrays.toString(cords));
        System.out.println(cords[0]);

        // Convertire le coordinate da stringa a intero.
        int x = Integer.parseInt(cords[0]);
        int y = Integer.parseInt(cords[1]);

        // Muovere il mouse in quelle coordinate.
        robot.mouseMove(x, y);
        // Cliccare il mouse in base a se è click destro o sinistro.
        if (direction == 'R') {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        } else {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }
}
