package com.awaker.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventRouter {
    private static final HashMap<GlobalEvent, List<EventReceiver>> listenerMap = new HashMap<>();

    /**
     * Registriert einen {@link EventReceiver} für globale Events
     *
     * @param receiver Der {@link EventReceiver}
     * @param event    Das {@link GlobalEvent}, das empfangen werden soll
     */
    public static void registerReceiver(EventReceiver receiver, GlobalEvent event) {
        if (!listenerMap.containsKey(event)) {
            listenerMap.put(event, new ArrayList<>());
        }
        listenerMap.get(event).add(receiver);
    }

    /**
     * Löst ein {@link GlobalEvent} aus.
     *
     * @param event Das auszulösende Event
     */
    public static void raiseEvent(GlobalEvent event) {
        List<EventReceiver> receivers = listenerMap.get(event);

        if (receivers != null) {
            for (EventReceiver receiver : receivers) {
                receiver.receiveGlobalEvent(event);
            }
        }
    }
}
