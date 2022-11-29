package com.jdoor;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSenderThread extends Thread{
    private DataOutputStream fileBytesWriter;
    private File file;

    public FileSenderThread(DataOutputStream fileBytesWriter) {
        this.fileBytesWriter = fileBytesWriter;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public DataOutputStream getFileBytesWriter() {
        return fileBytesWriter;
    }

    @Override
    public void run() {
        try {
            FileInputStream fileReader = new FileInputStream(file);
            byte[] fileBytes = fileReader.readAllBytes();
            fileReader.close();
            fileBytesWriter.write(fileBytes);
            fileBytesWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
