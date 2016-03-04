package com.awaker.server;

import com.awaker.data.TrackWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Soll alle Anfragen beantworten
 */
public class Server {
    public static final int PORT_NUMBER = 4732;
    private ServerSocket serverSocket;

    private boolean interrupt = false;

    ServerListener listener;

    public Server(ServerListener listener) {
        this.listener = listener;
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this::runServer).start();
    }

    public void stopServer() {
        interrupt = true;
    }

    /**
     * http://stackoverflow.com/questions/10618441/java-read-from-binary-file-send-bytes-over-socket
     * https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
     */
    private void runServer() {
        Socket clientSocket;
        while (!interrupt) {
            try {
                clientSocket = serverSocket.accept();
                System.out.println("new socket");

                while (!clientSocket.isClosed()) {
                    processSocket(clientSocket);
                }

                if (clientSocket.isClosed()) {
                    System.out.println("socket closed");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processSocket(Socket clientSocket) throws IOException {
        PrintWriter socketOut = new PrintWriter(clientSocket.getOutputStream(), true);

        InputStream socketIn = clientSocket.getInputStream();

        int readByte;
        StringBuilder sb = new StringBuilder();
        byte[] commandBuffer = new byte[100];
        int byteCount = 0;
        while ((readByte = socketIn.read()) > -1) {
            //10 entspricht dem Zeilenumbruch \n
            if (readByte == 10) {
                break;
            }

            commandBuffer[byteCount++] = (byte) readByte;
            if (byteCount == 100) {
                //Wenn buffer voll ist, dann leeren
                sb.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(commandBuffer)).toString());
                byteCount = 0;
            }
        }
        //Dekodieren mit UTF-8, ist etwa für Dateinamen nötig
        sb.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(commandBuffer, 0, byteCount)));

        String inputLine = sb.toString();
        boolean printStatus = true;

        String[] args = inputLine.split(";");
        switch (args[0]) {
            case "play":
                listener.play();
                break;

            case "playFromPosition":
                int pos = Integer.parseInt(args[1]);
                listener.playFromPosition(pos);
                break;

            case "pause":
                listener.pause();
                break;

            case "stop":
                listener.stop();
                break;

            case "togglePlayPause":
                listener.togglePlayPause();
                break;

            case "playFile":
                String title = args[1];
                String artist = args[2];

                if (!listener.playFile(new TrackWrapper(title, artist))) {
                    socketOut.println("file not found");
                    printStatus = false;
                }
                break;

            case "uploadAndPlayFile":
                String fileName = args[1];
                int fileLength = Integer.parseInt(args[2]);

                //abspielen
                listener.downloadFile(socketIn, fileLength, fileName, true);
                break;

            case "playNext":
                listener.playNext();
                break;

            case "playPrevious":
                listener.playPrevious();
                break;

            case "setShuffle":
                boolean shuffle = Boolean.parseBoolean(args[1]);
                listener.setShuffle(shuffle);
                break;

            case "setRepeatMode":
                int repeatMode = Integer.parseInt(args[1]);
                listener.setRepeatMode(repeatMode);
                break;

            case "setBrightness":
                int brightness = Integer.parseInt(args[1]);
                listener.setBrightness(brightness);
                break;

            case "changeVisualization":
                listener.changeVisualisation(args[1]);
                break;

            case "getStatus":
                //Status wird sowieso ausgegeben
                break;

            default:
                clientSocket.close();
        }

        if (printStatus) {
            socketOut.println(listener.getStatus());
        }
        System.out.println("Processed: " + inputLine);
    }

    public void closeSocket() {
        try {
            serverSocket.close();
            System.out.println("serversocket closed by kill");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
