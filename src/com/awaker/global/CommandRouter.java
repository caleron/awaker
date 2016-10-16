package com.awaker.global;

import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;

import java.util.HashMap;
import java.util.Map;

/**
 * Zentraler Router für Befehle
 */
public class CommandRouter {

    private static final Map<String, Command> commandMap = new HashMap<>();
    private static final Map<Command, CommandHandler> handlerMap = new HashMap<>();

    /**
     * Registriert einen Handler für eine Menge von Befehlen.
     *
     * @param t       Enum mit den unterstützten Aktionen.
     * @param handler Der Handler.
     */
    public static void registerHandler(Class<? extends Command> t, CommandHandler handler) {
        for (Command e : t.getEnumConstants()) {
            commandMap.put(e.getAction(), e);
            handlerMap.put(e, handler);
        }
    }

    /**
     * Löst alle Handler zu einem Befehl aus.
     *
     * @param command Der Befehl
     * @param data    Die dazugehörigen Daten
     * @return entsprechendes {@link Answer}-Objekt
     */
    public static Answer handleCommand(Command command, CommandData data) {
        return handleCommand(command, data, false);
    }

    /**
     * Löst alle Handler zu einem Befehl aus.
     *
     * @param command Der Befehl
     * @param data    Die dazugehörigen Daten
     * @return entsprechendes {@link Answer}-Objekt
     */
    public static Answer handleCommand(Command command, CommandData data, boolean buildAnswer) {
        CommandHandler commandHandler = handlerMap.get(command);
        if (commandHandler == null)
            return null;

        return commandHandler.handleCommand(command, data, buildAnswer);
    }

    /**
     * Löst alle Handler zu einem Befehl aus. Sollte nur für Befehle ohne zusätzliche Daten verwendet werden.
     *
     * @param command Der Befehl
     * @return entsprechendes {@link Answer}-Objekt
     */
    public static Answer handleCommand(Command command) {
        return handleCommand(command, false);
    }

    /**
     * Löst alle Handler zu einem Befehl aus. Sollte nur für Befehle ohne zusätzliche Daten verwendet werden.
     *
     * @param command Der Befehl
     * @return entsprechendes {@link Answer}-Objekt
     */
    public static Answer handleCommand(Command command, boolean buildAnswer) {
        return handleCommand(command, null, buildAnswer);
    }

    /**
     * Löst alle Handler zu einem Befehl aus.
     *
     * @param data Das Daten-Objekt
     * @return entsprechendes {@link Answer}-Objekt
     */
    public static Answer handleCommand(CommandData data, boolean buildAnswer) {
        Command command = commandMap.get(data.action);
        if (command == null) {
            return null;
        }

        CommandHandler commandHandler = handlerMap.get(command);
        if (commandHandler == null)
            return null;

        return commandHandler.handleCommand(command, data, buildAnswer);
    }
}
