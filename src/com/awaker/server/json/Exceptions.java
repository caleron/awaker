package com.awaker.server.json;

/**
 * Klassen, durch die außerordentliche Aktionen ausgelöst werden.
 */
public abstract class Exceptions {
    public static class Shutdown extends Throwable {

    }

    public static class CloseSocket extends Throwable {

    }
}
