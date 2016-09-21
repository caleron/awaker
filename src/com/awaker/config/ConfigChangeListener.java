package com.awaker.config;

@FunctionalInterface
public interface ConfigChangeListener {
    void configChanged(ConfigKey key);
}
