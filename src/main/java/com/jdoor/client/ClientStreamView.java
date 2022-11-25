package com.jdoor.client;

import com.jdoor.client.view.ScreenView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class ClientStreamView extends Thread{
    private DatagramSocket socketView;
    private int screenHeight, screenWidth;
    private ScreenView screenView;
    private boolean socketTimeout;
    private boolean connected;

    public ClientStreamView(int port) throws SocketException {
        socketView = new DatagramSocket(port);
        socketView.setSoTimeout(30000);
        socketTimeout = false;
        connected = true;
        screenHeight = 0;
        screenWidth = 0;
    }

    public void setScreenView(ScreenView screenView) {
        this.screenView = screenView;
    }

    public void setScreenDimension(String screenDimension) throws NumberFormatException{
        String[] dim = screenDimension.split("x");
        this.screenWidth = Integer.parseInt(dim[0]);
        this.screenHeight = Integer.parseInt(dim[1]);
        System.out.println(screenDimension);
    }
    public void setScreenDimension(int screenHeight, int screenWidth) {
        this.screenHeight = screenHeight;
    }

    public DatagramSocket getSocketView() {
        return socketView;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public boolean isSocketTimeout() {
        return socketTimeout;
    }

    @Override
    public void run() {
        while(connected) {
            try {
                DatagramPacket data = new DatagramPacket(new byte[(screenHeight * screenWidth) * 2], (screenHeight * screenWidth) * 2);
                socketView.receive(data);
                screenView.setScreen(data.getData());
                screenView.repaint();
            } catch(SocketTimeoutException e) {
               socketTimeout = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        socketView.close();
    }
}
