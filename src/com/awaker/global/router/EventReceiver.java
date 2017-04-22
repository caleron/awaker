package com.awaker.global.router;

@FunctionalInterface
public interface EventReceiver {
    void receiveGlobalEvent(GlobalEvent globalEvent);
}
