package com.awaker.global.router;

import java.util.*;

public class EventRouter {
    private static final HashMap<GlobalEvent, Set<EventReceiver>> listenerMap = new HashMap<>();

    /**
     * Registriert einen {@link EventReceiver} für globale Events
     *
     * @param receiver Der {@link EventReceiver}
     * @param event    Das {@link GlobalEvent}, das empfangen werden soll
     */
    public static void registerReceiver(EventReceiver receiver, GlobalEvent event) {
        if (!listenerMap.containsKey(event)) {
            listenerMap.put(event, new HashSet<>());
        }
        listenerMap.get(event).add(receiver);
    }

    /**
     * Löst ein {@link GlobalEvent} aus.
     *
     * @param event Das auszulösende Event
     */
    public static void raiseEvent(GlobalEvent event) {
        Set<EventReceiver> receivers = listenerMap.get(event);

        if (receivers != null) {
            for (EventReceiver receiver : receivers) {
                receiver.receiveGlobalEvent(event);
            }
        }
    }
}
