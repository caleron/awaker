package com.awaker.server;

import com.awaker.global.router.EventReceiver;
import com.awaker.global.router.EventRouter;
import com.awaker.global.router.GlobalEvent;
import com.awaker.util.Log;

/**
 * Dient als Schnittstelle zum WebSocketServer
 */
public class ServerManager implements EventReceiver {

    private final MyWebSocketServer server;

    public static void start() {
        new ServerManager();
    }

    private ServerManager() {
        server = new MyWebSocketServer();
        server.start();

        WebContentServer.start();
        HttpUploadServer.start();

        EventRouter.registerReceiver(this, GlobalEvent.PLAYBACK_NEW_SONG);
        EventRouter.registerReceiver(this, GlobalEvent.SHUTDOWN);
    }

    @Override
    public void receiveGlobalEvent(GlobalEvent globalEvent) {
        switch (globalEvent) {
            case PLAYBACK_NEW_SONG:
                server.sendStatus();
                break;
            case SHUTDOWN:
                try {
                    server.stop();
                } catch (Exception e) {
                    Log.error(e);
                }
                break;
        }
    }
}
