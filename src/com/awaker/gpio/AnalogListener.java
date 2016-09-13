package com.awaker.gpio;

/**
 * Stellt einen Listener für Veränderungen an analogen Steuerungselementen dar.
 */
public interface AnalogListener {
    /**
     * Setzt die Lautstärke
     *
     * @param volume Wert zwischen 0 und 100
     */
    void setVolume(int volume);

    /**
     * Setzt die Helligkeit des weißen Kanals
     *
     * @param brightness Wert zwischen 0 und 100
     */
    void setWhiteBrightness(int brightness, boolean smooth);

    /**
     * Setzt die Helligkeit der Farbanimationen
     *
     * @param brightness Wert zwischen 0 und 100
     */
    void setAnimationBrightness(int brightness, boolean smooth);

    /**
     * Setzt die Helligkeit des roten Kanals
     *
     * @param brightness Wert zwischen 0 und 100
     */
    void setRed(int brightness, boolean smooth);

    /**
     * Setzt die Helligkeit des grünen Kanals
     *
     * @param brightness Wert zwischen 0 und 100
     */
    void setGreen(int brightness, boolean smooth);

    /**
     * Setzt die Helligkeit des blauen Kanals
     *
     * @param brightness Wert zwischen 0 und 100
     */
    void setBlue(int brightness, boolean smooth);
}
