package com.jdoor.server.keyboard;

import java.awt.*;

/**
 * Classe per controllare la tastiera del server.
 * C'è nè solo una istanza per tutta l'esecuzione del programma.
 */
public class KeyboardController {
    private static KeyboardController currentInstance;

    private final Robot robot;

    /**
     * Costruttore della classe.
     * @throws AWTException Nel caso non si riesca ad accedere allo schermo (ad esempio in ambiente headless, vedi: <a href="https://stackoverflow.com/questions/13487025/headless-environment-error-in-java-awt-robot-class-with-mac-os">...</a>).
     */
    private KeyboardController() throws AWTException {
        robot = new Robot();
    }

    /**
     * Metodo per ottenere l'istanza principale della classe.
     * @return L'istanza corrente della classe.
     * @throws AWTException Nel caso non ri riesca ad accedere allo schermo (vedi costruttore).
     */
    public static synchronized KeyboardController getInstance() throws AWTException {
        if (currentInstance == null)
            currentInstance = new KeyboardController();
        return currentInstance;
    }

    /**
     * Metodo per premere un tasto sul server.
     * Questo metodo può essere chiamato da un solo thread alla volta.
     * Esempio comando: K4 -> Key 4
     * @param key Il comando col tasto da premere.
     */
    public synchronized void pressKeyboard(String key) {
        // Ottenere il codice del tasto da premere.
        String code = key.substring(1);
        // Convertire il codice da stringa ad intero.
        int intCode;
        try {
            intCode = Integer.parseInt(code);
        } catch (Exception e) {
            intCode = 0;
        }

        // Premere il tasto.
        robot.keyPress(intCode);
        robot.keyRelease(intCode);
    }
}
