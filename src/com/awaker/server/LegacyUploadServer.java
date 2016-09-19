package com.awaker.server;

import com.awaker.Awaker;
import com.awaker.util.Config;
import com.awaker.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Server zum Upload von Dateien
 */
public class LegacyUploadServer {
    private ServerSocket serverSocket;

    private boolean interrupt = false;

    private final ServerListener listener;

    public LegacyUploadServer(ServerListener listener) {
        this.listener = listener;
        try {
            serverSocket = new ServerSocket(Config.LEGACY_UPLOAD_PORT);
        } catch (IOException e) {
            Log.error(e);
        }

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
            if (serverSocket == null)
                return;

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
     * Liest einen Befehl vom Socket und führt ihn aus.
     *
     * @param clientSocket Der Socket.
     * @throws IOException
     */
    private void readCommand(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String json = reader.readLine();
        UploadCommand command = Awaker.GSON.fromJson(json, UploadCommand.class);

        listener.downloadFile(clientSocket.getInputStream(), command.fileLength, command.name, command.play);
    }

    /**
     * Klasse zum Deserialisieren von JSON
     */
    private static class UploadCommand {
        boolean play;
        int fileLength;
        String name;
    }
}
