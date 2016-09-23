package com.awaker.config;

/**
 *
 */
public enum ConfigKey {

    TIME_SERVER("time_server", "time-c.nist.gov"),
    DETECT_CLAPS("detect_claps", false),
    LIGHT_ON_SUNRISE("light_on_sunrise", false),
    SUNRISE_TIME_OFFSET("sunrise_time_offset", 0),
    LIGHT_ON_SUNSET("light_on_sunset", false),
    SUNSET_TIME_OFFSET("sunset_time_offset", 0);

    private String key;
    private Object def;

    ConfigKey(String key, String def) {
        this.key = key;
        this.def = def;
    }

    ConfigKey(String key, Boolean def) {
        this.key = key;
        this.def = def;
    }

    ConfigKey(String key, Integer def) {
        this.key = key;
        this.def = def;
    }

    public boolean accepts(Object obj) {
        if (obj == null)
            return false;

        if (def.getClass().equals(obj.getClass())) {
            return true;
        } else {
            String tmp = obj.toString().toLowerCase();
            if (def instanceof Boolean) {
                if (tmp.equals("true") || tmp.equals("false")) {
                    return true;
                }
            } else if (def instanceof Integer) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Integer.parseInt(tmp);
                    return true;
                } catch (Exception ignored) {
                }
            }
            return false;
        }
    }

    public String getDefault() {
        return def.toString();
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
