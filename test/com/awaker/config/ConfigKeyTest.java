package com.awaker.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigKeyTest {

    @Test
    public void run() {
        assertFalse(ConfigKey.LIGHT_ON_SUNRISE.accepts(123));
        assertTrue(ConfigKey.LIGHT_ON_SUNRISE.accepts(true));
        assertTrue(ConfigKey.LIGHT_ON_SUNRISE.accepts("true"));
        assertFalse(ConfigKey.LIGHT_ON_SUNRISE.accepts("hello"));

        assertTrue(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts(123));
        assertFalse(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts(true));
        assertFalse(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts("true"));
        assertFalse(ConfigKey.SUNSET_TIME_OFFSET_SECONDS.accepts("hello"));

        assertFalse(ConfigKey.TIME_SERVER.accepts(123));
        assertFalse(ConfigKey.TIME_SERVER.accepts(true));
        assertTrue(ConfigKey.TIME_SERVER.accepts("true"));
        assertTrue(ConfigKey.TIME_SERVER.accepts("hello"));
    }
}