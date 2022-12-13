package com.jdoor.client;

import com.jdoor.client.view.ClientFrame;

/**
 * Classe principale del client.
 */
public class MainClient  {

    public static void main(String[] args) {
        ClientFrame cFrame = new ClientFrame();
        new ConnectionManagement(cFrame);
    }
}
