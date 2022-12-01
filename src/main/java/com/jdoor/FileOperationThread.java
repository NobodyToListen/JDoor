package com.jdoor;

import java.io.*;

public class FileOperationThread extends Thread{
    private DataOutputStream sender;
    private DataInputStream inputStream;
    private File fileToTransfer;
    private boolean running;
    private boolean transferring;
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
    }

    private void receiveFile() throws IOException {
        FileOutputStream fileOutput = new FileOutputStream(fileToTransfer);
        fileOutput.write(inputStream.readAllBytes());
        fileOutput.close();
    }

    private void sendFile() throws IOException {
        FileInputStream fileInput = new FileInputStream(fileToTransfer);
        sender.write(fileInput.readAllBytes());
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    transferring = false;
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
