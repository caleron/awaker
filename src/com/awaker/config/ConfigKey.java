package com.awaker.config;

/**
 *
 */
public enum ConfigKey {

    DETECT_CLAPS("detect_claps", false),
    LIGHT_ON_SUNRISE("light_on_sunrise", false),
    SUNRISE_TIME_OFFSET("sunset_time_offset", 0),
    LIGHT_ON_SUNSET("light_on_sunset", false),
    SUNSET_TIME_OFFSET("sunset_time_offset", 0);

    private String key;
    private String def;

    ConfigKey(String key, String def) {
        this.key = key;
        this.def = def;
    }

    ConfigKey(String key, Boolean def) {
        this.key = key;
        this.def = def.toString();
    }

    ConfigKey(String key, Integer def) {
        this.key = key;
        this.def = def.toString();
    }

    public String getDefault() {
        return def;
    }

    public String getKey() {
        return key;
    }

    public static ConfigKey getForKey(String key) {
        ConfigKey[] values = ConfigKey.values();
        for (ConfigKey value : values) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        return null;
    }
}
