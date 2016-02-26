package com.awaker.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

                while (!clientSocket.isClosed()) {
                    processSocket(clientSocket);
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
        while ((readByte = socketIn.read()) > -1) {
            char b = ((char) ((byte) readByte));
            if (b == '\n') {
                break;
            }
            sb.append(b);
        }

        String inputLine = sb.toString();

        String[] args = inputLine.split(";");
        switch (args[0]) {
            case "play":
                listener.play();
                socketOut.println("playing");
                break;
            case "playFromPosition":
                int pos = Integer.parseInt(args[1]);
                listener.playFromPosition(pos);
                socketOut.println("playing");
                break;
            case "pause":
                listener.pause();
                socketOut.println("paused");
                break;
            case "stop":
                listener.stop();
                socketOut.println("stopped");
                break;
            case "playFile":
                if (!listener.playFile(args[1])) {
                    socketOut.println("file not found");
                }
                socketOut.println("playing");
                break;
            case "uploadAndPlayFile":
                String fileName = args[1];
                int fileLength = Integer.parseInt(args[2]);

                final int BUFFER_SIZE = 8096;
                byte[] buffer = new byte[BUFFER_SIZE];

                FileOutputStream fos = new FileOutputStream(fileName);

                //Anzahl gelesener Bytes beim letzten Aufruf von read()
                int readCount = socketIn.read(buffer);
                //Insgesamt gelesene Bytes
                int totalBytesRead = readCount;
                while (readCount > 0) {
                    //Gelesene Bytes schreiben
                    fos.write(buffer, 0, readCount);

                    if (fileLength - totalBytesRead < BUFFER_SIZE) {
                        //Nur so viele Bytes lesen wie nötig
                        readCount = socketIn.read(buffer, 0, fileLength - totalBytesRead);
                    } else {
                        readCount = socketIn.read(buffer);
                    }
                    totalBytesRead += readCount;
                }
                //fertig
                fos.close();
                //abspielen
                listener.playFile(fileName);
                socketOut.println("playing");
                break;
            case "setBrightness":
                int brightness = Integer.parseInt(args[1]);
                listener.setBrightness(brightness);
                socketOut.println("success");
                break;
            case "changeVisualization":
                listener.changeVisualisation(args[1]);
                socketOut.println("success");
                break;
            case "getStatus":
                String status = listener.getStatus();
                socketOut.println(status);
                break;
            default:
                clientSocket.close();
        }
    }
}
