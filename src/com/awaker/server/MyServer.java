package com.awaker.server;


import com.awaker.server.json.Answer;
import com.awaker.server.json.Command;
import com.awaker.server.json.Exceptions;
import com.awaker.util.Config;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Server für WebSockets. Übernimmt die Abarbeitung aller Anfragen außer Uploads.
 */
public class MyServer extends WebSocketServer {
    private final Gson gson;
    private final ServerListener listener;

    //Timer, der 500ms nach dem letzten Befehl den Status sendet.
    private Timer timer = new Timer(500, e -> sendStatus());

    //nach 2 Sekunden sicher den Status senden, unabhängig von aufeinanderfolgenden Anfragen
    private Timer longerTimer = new Timer(2000, e -> sendStatus());

//TODO status bei neuem song pushen, statt clients abfragen zu lassen

    /**
     * Erstellt eine neue Instanz.
     *
     * @param listener Der {@link ServerListener}.
     */
    public MyServer(ServerListener listener) {
        super(new InetSocketAddress(Config.WEBSOCKET_PORT));
        this.listener = listener;
        gson = new Gson();

        timer.setRepeats(false);
        longerTimer.setRepeats(false);
    }

    /**
     * Sendet den aktuellen Status an alle aktuellen Clients.
     */
    public void sendStatus() {
        longerTimer.stop();
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

        if (connections().size() > 1) {
            timer.restart();
            longerTimer.start();
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
