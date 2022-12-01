package com.jdoor;

import java.io.*;

public class FileOperationThread extends Thread{
    private DataOutputStream sender;
    private DataInputStream inputStream;
    private File fileToTransfer;
    private boolean running;
    private boolean transferring;
    private BufferedOutputStream fileOutput;
    private BufferedInputStream fileInput;
    private Constants.FileOperations operation;

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

    public void setOperation(Constants.FileOperations operation) {
        this.operation = operation;
        transferring = true;
    }

    public void setFileToTransfer(File fileToTransfer) throws FileNotFoundException {
        this.fileToTransfer = fileToTransfer;
        fileOutput = new BufferedOutputStream(new FileOutputStream(fileToTransfer));
        fileInput = new BufferedInputStream(new FileInputStream(fileToTransfer));
    }

    private void receiveFile() throws IOException {
        int bytes = 0;

        long size = inputStream.readLong();
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = inputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutput.write(buffer, 0, bytes);
            size -= bytes;
        }
        System.out.println("File is Received");
        fileOutput.close();
    }

    private void sendFile() throws IOException {
        int bytes = 0;
        sender.writeLong(fileToTransfer.length());
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInput.read(buffer)) != -1) {
            sender.write(buffer, 0, bytes);
            sender.flush();
        }
        fileInput.close();
    }
    @Override
    public void run() {
        while(running) {
            if(transferring) {
                try {
                    if(operation == Constants.FileOperations.Get) {
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
