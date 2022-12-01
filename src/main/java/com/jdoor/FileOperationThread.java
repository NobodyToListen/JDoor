package com.jdoor;

import java.io.*;

public class FileOperationThread extends Thread{
    private DataOutputStream sender;
    private DataInputStream inputStream;
    private File fileToTransfer;
    private boolean running;
    private boolean transferring;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
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
        fileOutputStream = new FileOutputStream(fileToTransfer);
        fileInputStream = new FileInputStream(fileToTransfer);
    }

    private void receiveFile() throws IOException {
        System.err.println("Sto ricevendo\n");
        byte[] fileBytes = inputStream.readAllBytes();
        System.err.println("Ho ricevuto\n");
        System.err.println("sto scrivendo\n");
        fileOutputStream.write(fileBytes);
        fileOutputStream.flush();
        System.err.println("ho scritto\n");
        fileOutputStream.close();
    }

    private void sendFile() throws IOException {
        System.err.println("Sto leggendo\n");
        byte[] fileBytes = fileInputStream.readAllBytes();
        System.err.println("ho letto\n");
        System.err.println("sto scrivendo su file\n");
        sender.write(fileBytes);
        sender.flush();
        System.err.println("ho scritto su file\n");
        fileInputStream.close();
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
