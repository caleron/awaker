package com.awaker.util;


import com.awaker.data.DbManager;

import java.util.HashMap;

public class Config {
    public static final String DETECT_CLAPS = "detect_claps";
    public static final String LIGHT_ON_SUNRISE = "light_on_sunrise";
    public static final String LIGHT_ON_SUNSET = "light_on_sunset";

    private static final String TABLE_NAME = "config";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private static HashMap<String, String> config;

    public static void init() {
        config = DbManager.getConfig();
    }

    public static void setString(String key, String value) {
        DbManager.setConfig(key, value);
        config.put(key, value);
    }

    public static void setBool(String key, Boolean value) {
        setString(key, value.toString());
    }

    public static void setInt(String key, Integer value) {
        setString(key, value.toString());
    }

    public static String getString(String key, String def) {
        if (config.containsKey(key)) {
            return config.get(key);
        }
        return def;
    }

    public static Boolean getBool(String key, Boolean def) {
        return Boolean.parseBoolean(getString(key, def.toString()));
    }

    public static Integer getInt(String key, Integer def) {
        return Integer.valueOf(getString(key, def.toString()));
    }

    public static String getCreateTableSQL() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" TEXT PRIMARY KEY," +
                "\"%s\" TEXT)", TABLE_NAME, NAME, VALUE);
    }
}
