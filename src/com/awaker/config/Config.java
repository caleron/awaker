package com.awaker.config;


import com.awaker.data.DbManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Config {
    private static final String TABLE_NAME = "config";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private static HashMap<String, String> config;
    private static HashMap<ConfigChangeListener, ConfigKey[]> listeners;

    public static void init() {
        config = DbManager.getConfig();
        listeners = new HashMap<>();
    }

    public static void addListener(ConfigChangeListener listener, ConfigKey key) {
        addListener(listener, new ConfigKey[]{key});
    }

    public static void addListener(ConfigChangeListener listener, ConfigKey[] key) {
        listeners.put(listener, key);
    }

    public static boolean set(ConfigKey key, Object value) {
        if (key == null)
            return false;

        if (!key.accepts(value))
            return false;

        DbManager.setConfig(key.getKey(), value.toString());
        config.put(key.getKey(), value.toString());

        //Events feuern
        listeners.forEach((listener, configKeys) -> {
            Arrays.stream(configKeys);
            if (Arrays.asList(configKeys).contains(key)) {
                new Thread(() -> listener.configChanged(key)).start();
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

    public static String[] getConfigOptions() {
        ConfigKey[] values = ConfigKey.values();
        ArrayList<String> list = new ArrayList<>();
        for (ConfigKey value : values) {
            list.add(value.getKey());
        }
        return list.toArray(new String[list.size()]);
    }

    public static HashMap<String, String> getConfig() {
        return config;
    }

    public static String getCreateTableSQL() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" TEXT PRIMARY KEY," +
                "\"%s\" TEXT)", TABLE_NAME, NAME, VALUE);
    }
}
