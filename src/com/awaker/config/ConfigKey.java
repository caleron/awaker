package com.awaker.config;

/**
 *
 */
public enum ConfigKey {

    SHUFFLE("shuffle", true),
    REPEAT_MODE("repeat_mode", "all", new String[]{"all", "track", "none"}),

    TIME_SERVER("time_server", "time-c.nist.gov", null),
    DETECT_CLAPS("detect_claps", false),
    LIGHT_ON_SUNRISE("light_on_sunrise", false),
    SUNRISE_TIME_OFFSET_SECONDS("sunrise_time_offset_seconds", 0),
    LIGHT_ON_SUNSET("light_on_sunset", false),
    SUNSET_TIME_OFFSET_SECONDS("sunset_time_offset_seconds", 0);

    private String key;
    private Object def;
    private String[] possibleValues = null;

    ConfigKey(String key, String def, String[] possibleValues) {
        this.key = key;
        this.def = def;
        this.possibleValues = possibleValues;
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
            if (def instanceof String && possibleValues != null) {
                //Falls nur bestimmte Werte erlaubt sind, hier überprüfen
                for (String value : possibleValues) {
                    if (value.equals(obj)) {
                        return true;
                    }
                }
                return false;
            }
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
