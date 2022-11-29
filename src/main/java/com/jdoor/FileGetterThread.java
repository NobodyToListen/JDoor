package com.jdoor;

import java.io.*;

public class FileGetterThread extends Thread{
    private DataInputStream fileBytesReader;
    private String defaultFilePath;

    public FileGetterThread(DataInputStream fileBytesReader) {
        this.fileBytesReader = fileBytesReader;
    }

    public void setDefaultFilePath(String defaultFilePath) {
        this.defaultFilePath = defaultFilePath;
    }

    public DataInputStream getFileBytesReader() {
        return fileBytesReader;
    }

    @Override
    public void run() {
        File file = new File(defaultFilePath);


        try {
            FileOutputStream fileWriter = new FileOutputStream(file);
            byte[] fileBytes = fileBytesReader.readAllBytes();
            fileWriter.write(fileBytes);
            fileWriter.flush();
            fileWriter.close();
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
