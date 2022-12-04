package com.jdoor.server.commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Thread per eseguire un comando come se fosse in un terminale.
 */
public class CommandControllerThread extends Thread {
    private final BufferedWriter bw;
    private final String command;

    /**
     * Costruttore della classe.
     * @param bw Il BufferedWriter del client dove mandare l'output.
     * @param command Il comando da eseguire.
     */
    public CommandControllerThread(BufferedWriter bw, String command) {
        this.bw = bw;

        this.command = command.substring(1);
    }

    /**
     * Metodo principale del thread in cui eseguire il comando.
     */
    @Override
    public void run() {
        // Ottenere il Runtime per eseguire il comando.
        Runtime runtime = Runtime.getRuntime();
        // Creare lo StringBuilder per costruire l'output del comando.
        StringBuilder builder = new StringBuilder();
        try {
            // Adesso eseguiamo il comando e catturiamo l'output con il BufferedReader che creiamo qui.
            BufferedReader processReader = new BufferedReader(new InputStreamReader(runtime.exec(command).getInputStream()));
            String line;

            // Otteniamo l'output linea per linea e lo aggiungiamo allo StringBuilder.
            while ((line = processReader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            builder.append("\n");
        } catch (IOException e) {
            builder.append(e.getMessage()).append("\n");
        }

        try {
            // Mandare l'output al client.
            bw.write(Base64.getEncoder().encodeToString(builder.toString().getBytes(StandardCharsets.UTF_8)) + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
