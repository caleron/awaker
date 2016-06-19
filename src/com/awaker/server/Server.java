package com.awaker.server;

import com.awaker.server.json.Answer;
import com.awaker.server.json.Command;
import com.awaker.server.json.Exceptions;
import com.awaker.util.Log;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Soll alle Anfragen beantworten
 */
@SuppressWarnings("Duplicates")
public class Server {
    private static final int PORT_NUMBER = 4732;
    private ServerSocket serverSocket;

    private boolean interrupt = false;

    private final ServerListener listener;
    private final Gson gson;

    public Server(ServerListener listener) {
        this.listener = listener;
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch (IOException e) {
            Log.error(e);
        }

        gson = new Gson();

        new Thread(this::runServer).start();
    }

    public void stopServer() {
        interrupt = true;
    }

    /**
     * Wartet in einer Endlosschleife auf eingehende Verbindungen und starten für jeden Socket einen eigenen Thread.
     * <p>
     * http://stackoverflow.com/questions/10618441/java-read-from-binary-file-send-bytes-over-socket
     * https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
     */
    private void runServer() {
        while (!interrupt) {
            try {
                Socket clientSocket = serverSocket.accept();

                //Neuen Thread für den Socket starten
                new Thread(() -> processSocket(clientSocket)).start();

            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    /**
     * Behandelt einen Socket. Dabei werden Befehle so lange beantwortet, bis der Socket geschlossen wird.
     *
     * @param clientSocket Der Socket.
     */
    private void processSocket(Socket clientSocket) {
        try {
            Log.message("new socket");

            //10 Sekunden timeout setzen
            clientSocket.setSoTimeout(10000);

            while (!clientSocket.isClosed()) {
                readCommand(clientSocket);
            }

        } catch (SocketTimeoutException e) {
            //Falls der 10-Sekunden-Timeout überschritten wurde, socket schließen
            try {
                clientSocket.close();
                Log.message("socket closed due to timeout");
            } catch (IOException e1) {
                Log.error(e1);
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * Liest einen einzelnen Befehl aus dem Socket und verarbeitet diesen.
     *
     * @param clientSocket Der Socket
     * @throws IOException
     */
    private void readCommand(Socket clientSocket) throws IOException {
        PrintWriter socketOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

        DataInputStream socketIn = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

        int commandLength = socketIn.readInt();

        byte[] commandBytes = new byte[commandLength];
        int readBytes = socketIn.read(commandBytes);

        if (readBytes != commandLength) {
            Log.message("unexspected end of stream during command reading");
        }

        //Dekodieren mit UTF-8, ist etwa für Dateinamen nötig
        String inputLine = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(commandBytes)).toString();

        Answer answer = processCommand(clientSocket, socketIn, inputLine);

        //antwort schicken
        if (answer != null) {
            socketOut.println(gson.toJson(answer));
        }
    }

    /**
     * Verarbeitet einen Befehl und gibt die zu sendende Antwort zurück.
     *
     * @param clientSocket Der Socket.
     * @param socketIn     Der InputStream des Sockets.
     * @param data         Die gelesenen Daten.
     * @return Die zu sendende Antwort.
     */
    private Answer processCommand(Socket clientSocket, InputStream socketIn, String data) {
        Command command = gson.fromJson(data, Command.class);

        try {
            return command.execute(listener, socketIn);
        } catch (Exceptions.CloseSocket e) {
            //Socket schließen
            try {
                clientSocket.close();
            } catch (IOException e1) {
                Log.error(e1);
            }
        } catch (Exceptions.Shutdown e) {
            //Server schließen
            try {
                clientSocket.close();
                serverSocket.close();
                listener.shutdown();
            } catch (IOException e1) {
                Log.error(e);
            }
        }

        return null;
    }
}
