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

    public static void setString(ConfigKey key, String value) {
        if (key == null)
            return;

        DbManager.setConfig(key.getKey(), value);
        config.put(key.getKey(), value);

        //Events feuern
        listeners.forEach((listener, configKeys) -> {
            Arrays.stream(configKeys);
            if (Arrays.asList(configKeys).contains(key)) {
                new Thread(() -> listener.configChanged(key)).start();
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
        if (key != null && config.containsKey(key.getKey())) {
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
