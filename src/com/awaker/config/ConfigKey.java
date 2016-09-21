package com.awaker.config;

/**
 *
 */
public enum ConfigKey {

    DETECT_CLAPS("detect_claps"),
    LIGHT_ON_SUNRISE("light_on_sunrise"),
    SUNRISE_TIME_OFFSET("sunset_time_offset"),
    LIGHT_ON_SUNSET("light_on_sunset"),
    SUNSET_TIME_OFFSET("sunset_time_offset");

    private String key;

    ConfigKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
