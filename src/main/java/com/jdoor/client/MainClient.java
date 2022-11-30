package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

public class MainClient{
    public static void main(String[] args) {
        ClientFrame cFrame = new ClientFrame();
        ConnectionManagement connectionManagement = new ConnectionManagement(cFrame);

    }
}
