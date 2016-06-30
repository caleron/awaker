package com.awaker.server;


import com.awaker.server.json.Answer;
import com.awaker.server.json.Command;
import com.awaker.server.json.Exceptions;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Server für WebSockets. Übernimmt die Abarbeitung aller Anfragen außer Uploads.
 */
public class MyServer extends WebSocketServer {
    private static final int PORT = 4733;
    private final Gson gson;
    private final ServerListener listener;

    /**
     * Erstellt eine neue Instanz.
     *
     * @param listener Der {@link ServerListener}.
     */
    public MyServer(ServerListener listener) {
        super(new InetSocketAddress("localhost", PORT));
        this.listener = listener;
        gson = new Gson();
    }

    /**
     * Sendet den aktuellen Status an alle aktuellen Clients.
     */
    public void sendStatus() {
        Answer answer = listener.getStatus(Answer.status());
        Collection<WebSocket> connections = connections();
        String statusString = gson.toJson(answer);

        for (WebSocket connection : connections) {
            connection.send(statusString);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("open");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("close");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println(message);
        Command command = gson.fromJson(message, Command.class);

        try {
            conn.send(gson.toJson(command.execute(listener)));
        } catch (Exceptions.CloseSocket closeSocket) {
            conn.close();
        } catch (Exceptions.Shutdown shutdown) {
            conn.close();
            listener.shutdown();
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("binary message");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        System.out.println("error");
    }
}
