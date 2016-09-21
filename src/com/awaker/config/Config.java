package com.awaker.config;


import com.awaker.data.DbManager;

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

    public static void setString(ConfigKey key, String value) {
        DbManager.setConfig(key.getKey(), value);
        config.put(key.getKey(), value);

        //Events feuern
        listeners.forEach((listener, configKeys) -> {
            Arrays.stream(configKeys);
            if (Arrays.asList(configKeys).contains(key)) {
                listener.configChanged(key);
            }
        });
    }

    public static void setBool(ConfigKey key, Boolean value) {
        setString(key, value.toString());
    }

    public static void setInt(ConfigKey key, Integer value) {
        setString(key, value.toString());
    }

    public static String getString(ConfigKey key, String def) {
        if (config.containsKey(key.getKey())) {
            return config.get(key.getKey());
        }
        return def;
    }

    public static Boolean getBool(ConfigKey key, Boolean def) {
        return Boolean.parseBoolean(getString(key, def.toString()));
    }

    public static Integer getInt(ConfigKey key, Integer def) {
        return Integer.valueOf(getString(key, def.toString()));
    }

    public static ConfigKey[] getConfigOptions() {
        return ConfigKey.values();
    }

    public static String getCreateTableSQL() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" TEXT PRIMARY KEY," +
                "\"%s\" TEXT)", TABLE_NAME, NAME, VALUE);
    }
}
