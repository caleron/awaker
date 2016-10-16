package com.awaker.config;


import com.awaker.data.DbManager;
import com.awaker.global.Command;
import com.awaker.global.CommandHandler;
import com.awaker.global.CommandRouter;
import com.awaker.server.json.Answer;
import com.awaker.server.json.JsonCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Config implements CommandHandler {
    private static final String TABLE_NAME = "config";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private static HashMap<String, String> config;
    //TODO map umdrehen, zu ConfigKey, ConfigChangeListener
    private static HashMap<ConfigChangeListener, ConfigKey[]> listeners;
    /**
     * Listener, die Synchron aufgerufen werden sollen
     */
    private static HashMap<ConfigChangeListener, ConfigKey[]> listenersSync;

    public static void init() {
        config = DbManager.getConfig();
        listeners = new HashMap<>();
        listenersSync = new HashMap<>();
        CommandRouter.registerHandler(ConfigCommand.class, new Config());
    }

    @Override
    public Answer handleCommand(Command command, JsonCommand data) {
        if (!(command instanceof ConfigCommand)) {
            throw new RuntimeException("Received Wrong Command");
        }

        ConfigCommand cmd = (ConfigCommand) command;

        Answer answer = Answer.config();
        switch (cmd) {
            case GET_CONFIG:
                answer.name = data.name;
                answer.value = getString(ConfigKey.getForKey(data.name));
                break;
            case GET_CONFIG_LIST:
                answer.config = getConfig();
                break;
            case GET_CONFIG_OPTIONS:
                answer.configOptions = getConfigOptions();
                break;
            case SET_CONFIG:
                answer.name = data.name;
                ConfigKey key = ConfigKey.getForKey(data.name);
                set(key, data.value);
                answer.value = getString(key);
                break;
        }
        return answer;
    }

    public static void addListener(ConfigChangeListener listener, ConfigKey key) {
        addListener(listener, new ConfigKey[]{key});
    }

    public static void addSyncListener(ConfigChangeListener listener, ConfigKey key) {
        addSyncListener(listener, new ConfigKey[]{key});
    }

    public static void addListener(ConfigChangeListener listener, ConfigKey[] key) {
        listeners.put(listener, key);
    }

    public static void addSyncListener(ConfigChangeListener listener, ConfigKey[] key) {
        listenersSync.put(listener, key);
    }

    public static boolean set(ConfigKey key, Object value) {
        if (key == null)
            return false;

        if (!key.accepts(value))
            return false;

        if (value.toString().equals(getString(key)))
            return false;

        DbManager.setConfig(key.getKey(), value.toString());
        config.put(key.getKey(), value.toString());

        //Events feuern
        listeners.forEach((listener, configKeys) -> {
            if (Arrays.asList(configKeys).contains(key)) {
                new Thread(() -> listener.configChanged(key)).start();
            }
        });
        //ohne Thread
        listenersSync.forEach((listener, configKeys) -> {
            if (Arrays.asList(configKeys).contains(key)) {
                listener.configChanged(key);
            }
        });
        return true;
    }

    public static String getString(ConfigKey key) {
        if (key == null)
            return "";

        if (config.containsKey(key.getKey())) {
            return config.get(key.getKey());
        }
        return key.getDefault();
    }

    public static Boolean getBool(ConfigKey key) {
        return Boolean.parseBoolean(getString(key));
    }

    public static Integer getInt(ConfigKey key) {
        return Integer.valueOf(getString(key));
    }

    private static String[] getConfigOptions() {
        ConfigKey[] values = ConfigKey.values();
        ArrayList<String> list = new ArrayList<>();
        for (ConfigKey value : values) {
            list.add(value.getKey());
        }
        return list.toArray(new String[list.size()]);
    }

    private static HashMap<String, String> getConfig() {
        return config;
    }

    public static String getCreateTableSQL() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" TEXT PRIMARY KEY," +
                "\"%s\" TEXT)", TABLE_NAME, NAME, VALUE);
    }
}
