package com.jdoor;

import java.io.*;

public class FileOperationThread extends Thread{
    private DataOutputStream sender;
    private DataInputStream inputStream;
    private File fileToTransfer;
    private boolean running;
    private boolean transferring;
    public static enum Operations {
        Send,
        Get
    }
    private Operations operation;

    public FileOperationThread(DataOutputStream sender, DataInputStream inputStream) {
        this.sender = sender;
        this.inputStream = inputStream;
        running = true;
        transferring = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isTransferring() {
        return transferring;
    }

    public void setOperation(Operations operation) {
        if(operation == Operations.Get || operation == Operations.Send) {
            this.operation = operation;
            transferring = true;
        }
    }

    public void setFileToTransfer(File fileToTransfer) {
        this.fileToTransfer = fileToTransfer;
    }

    private void receiveFile() throws IOException {
        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(fileToTransfer);

        long size = inputStream.readLong();
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = inputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;
        }
        fileOutputStream.close();
    }

    private void sendFile() throws IOException {
        int bytes = 0;
        FileInputStream fileInputStream = new FileInputStream(fileToTransfer);

        sender.writeLong(fileToTransfer.length());

        byte[] buffer = new byte[4096];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            sender.write(buffer, 0, bytes);
            sender.flush();
        }
        fileInputStream.close();
    }
    @Override
    public void run() {
        while(running) {
            if(transferring) {
                try {
                    if(operation == Operations.Get) {
                        receiveFile();
                    } else {
                        sendFile();
                    }
                    transferring = false;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            sender.close();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
