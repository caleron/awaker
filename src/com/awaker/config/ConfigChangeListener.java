package com.awaker.config;

@FunctionalInterface
public interface ConfigChangeListener {
    /**
     * Wird ausgelöst, wenn sich eine Konfiguration ändert, für die sich ein Objekt mit Config.addListener
     * registriert hat.
     *
     * @param key Der Schlüssel, von dem sich der Wert geändert hat.
     */
    void configChanged(ConfigKey key);
}
