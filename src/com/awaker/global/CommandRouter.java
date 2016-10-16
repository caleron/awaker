package com.awaker.global;

import com.awaker.server.json.Answer;
import com.awaker.server.json.JsonCommand;

import java.util.HashMap;
import java.util.Map;

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

    public static Answer handleCommand(Command command, JsonCommand data) {
        CommandHandler commandHandler = handlerMap.get(command);
        if (commandHandler == null)
            return null;

        return commandHandler.handleCommand(command, data);
    }

    public static Answer handleCommand(Command command) {
        return handleCommand(command, null);
    }

    public static Answer handleCommand(JsonCommand data) {
        Command command = commandMap.get(data.action);
        if (command == null) {
            return null;
        }

        CommandHandler commandHandler = handlerMap.get(command);
        if (commandHandler == null)
            return null;

        return commandHandler.handleCommand(command, data);
    }
}
