package com.awaker.test;

import com.awaker.config.ConfigKey;

class Test {
    public static void main(String[] args) {
        System.out.println(ConfigKey.LIGHT_ON_SUNRISE.accepts(123));
        System.out.println(ConfigKey.LIGHT_ON_SUNRISE.accepts(true));
        System.out.println(ConfigKey.LIGHT_ON_SUNRISE.accepts("true"));
        System.out.println(ConfigKey.LIGHT_ON_SUNRISE.accepts("hello"));

        System.out.println(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts(123));
        System.out.println(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts(true));
        System.out.println(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts("true"));
        System.out.println(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts("hello"));

        System.out.println(ConfigKey.TIME_SERVER.accepts(123));
        System.out.println(ConfigKey.TIME_SERVER.accepts(true));
        System.out.println(ConfigKey.TIME_SERVER.accepts("true"));
        System.out.println(ConfigKey.TIME_SERVER.accepts("hello"));
    }
}
