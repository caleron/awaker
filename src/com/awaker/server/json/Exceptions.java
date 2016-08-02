package com.awaker.server.json;

/**
 * Klassen, durch die außerordentliche Aktionen ausgelöst werden.
 */
public abstract class Exceptions {
    public static class ShutdownServer extends Throwable {

    }

    public static class CloseSocket extends Throwable {

    }

    public static class ShutdownRaspi extends Throwable {

    }

    public static class RebootServer extends Throwable {

    }

    public static class RebootRaspi extends Throwable {

    }
}
