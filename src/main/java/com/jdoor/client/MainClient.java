package com.jdoor.client;

import com.jdoor.FileSenderThread;
import com.jdoor.client.view.ClientFrame;

import java.awt.event.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jdoor.Constants.TCP_PORT;
import static com.jdoor.Constants.UDP_PORT;

public class MainClient{
    public static void main(String[] args) {
        ClientFrame cFrame = new ClientFrame();
        ConnectionManagement connectionManagement = new ConnectionManagement(cFrame);

    }
}
