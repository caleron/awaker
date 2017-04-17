package com.awaker.server;


import com.awaker.config.PortConfig;
import com.awaker.global.router.CommandRouter;
import com.awaker.global.DataCommand;
import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;
import com.awaker.util.Log;
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
public class MyWebSocketServer extends WebSocketServer {
    private final Gson gson;

    //Timer, der 500ms nach dem letzten Befehl den Status sendet.
    private Timer timer = new Timer(500, e -> sendStatus());

    //nach 2 Sekunden sicher den Status senden, unabhängig von aufeinanderfolgenden Anfragen
    private Timer longerTimer = new Timer(2000, e -> sendStatus());

    /**
     * Erstellt eine neue Instanz.
     * Verschlüsselung einführen, wenn ssl-server verwendet wird
     * http://stackoverflow.com/questions/9745249/html5-websocket-with-ssl
     */
    public MyWebSocketServer() {
        super(new InetSocketAddress(PortConfig.WEBSOCKET_PORT));
        gson = new Gson();

        timer.setRepeats(false);
        longerTimer.setRepeats(false);
    }

    /**
     * Sendet den aktuellen Status an alle aktuellen Clients.
     */
    void sendStatus() {
        longerTimer.stop();
        Answer answer = CommandRouter.handleCommand(DataCommand.GET_STATUS);
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
        CommandData commandData = gson.fromJson(message, CommandData.class);

        try {
            Answer answer = CommandRouter.handleCommand(commandData, true);
            if (answer != null) {
                conn.send(gson.toJson(answer));
            }
        } catch (Exception e) {
            Log.error(e);
        }

        if (connections().size() > 1) {
            timer.restart();
            longerTimer.start();
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Log.message("binary message");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        Log.error(ex);
    }
}
