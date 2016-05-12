package com.awaker.server;

import com.awaker.data.TrackWrapper;
import com.awaker.util.Log;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Soll alle Anfragen beantworten
 */
public class Server {
    private static final int PORT_NUMBER = 4732;
    private ServerSocket serverSocket;

    private boolean interrupt = false;

    private final ServerListener listener;

    public Server(ServerListener listener) {
        this.listener = listener;
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch (IOException e) {
            Log.error(e);
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
        Socket clientSocket = null;
        while (!interrupt) {
            try {
                clientSocket = serverSocket.accept();
                Log.message("new socket");

                //10 Sekunden timeout setzen
                clientSocket.setSoTimeout(10000);

                while (!clientSocket.isClosed()) {
                    processSocket(clientSocket);
                }

                if (clientSocket.isClosed()) {
                    Log.message("socket closed");
                }
            } catch (SocketTimeoutException e) {
                //Falls der 10-Sekunden-Timeout überschritten wurde, socket schließen
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                        Log.message("socket closed due to timeout");
                    } catch (IOException e1) {
                        Log.error(e1);
                    }
                }
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    private void processSocket(Socket clientSocket) throws IOException {
        PrintWriter socketOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

        BufferedInputStream socketIn = new BufferedInputStream(clientSocket.getInputStream());

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
        //args[0] ist die Aktion, die nächsten args die Argumente/Parameter
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

            case "setVolume":
                int volume = Integer.parseInt(args[1]);
                listener.setVolume(volume);
                break;

            case "setWhiteBrightness":
                int brightness = Integer.parseInt(args[1]);
                listener.setWhiteBrightness(brightness);
                break;

            case "setColorBrightness":
                brightness = Integer.parseInt(args[1]);
                listener.setColorBrightness(brightness);
                break;

            case "setColorMode":
                String mode = args[1];
                listener.setColorMode(mode);
                break;

            case "setColor":
                int color = Integer.parseInt(args[1]);
                listener.setColor(new Color(color, false));

                break;

            case "setRGBColor":
                int red = Integer.parseInt(args[1]);
                int green = Integer.parseInt(args[2]);
                int blue = Integer.parseInt(args[3]);

                listener.setColor(new Color(red, green, blue));
                break;

            case "changeVisualization":
                listener.changeVisualisation(args[1]);
                break;

            case "getStatus":
                //Status wird sowieso ausgegeben
                break;

            case "sendString":
                int length = Integer.parseInt(args[1]);
                listener.stringReceived(readString(socketIn, length));
                break;
            case "shutdown":
                clientSocket.close();
                serverSocket.close();
                listener.shutdown();

                break;
            default:
                clientSocket.close();
        }

        if (clientSocket.isClosed())
            return;

        if (printStatus) {
            String status = listener.getStatus();
            socketOut.println(status);
            Log.message("Processed: " + inputLine + ", Status: " + status);
        } else {
            Log.message("Processed: " + inputLine);
        }
    }

    public void closeSocket() {
        try {
            serverSocket.close();
            Log.message("serversocket closed by kill");
        } catch (IOException e) {
            Log.error(e);
        }
    }

    private static String readString(InputStream is, int length) throws IOException {
        byte[] bytes = new byte[length];

        int readByte;
        StringBuilder sb = new StringBuilder();
        int byteCount = 0;
        while ((readByte = is.read()) > -1) {
            if (byteCount >= length)
                break;

            bytes[byteCount++] = (byte) readByte;
        }
        //Dekodieren mit UTF-8
        sb.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, 0, byteCount)));
        return sb.toString();
    }
}
