package com.awaker.gpio.adc;

/**
 * Ein Listener für Veränderungen an analogen Eingängen.
 */
public interface AdcListener {
    /**
     * Wird ausgelöst, wenn sich ein Wert verändert hat. Der Wert von newValue liegt im Bereich von 0 bis 1023.
     *
     * @param channel  Der Kanal, auf dem sich der Wert verändert hat.
     * @param newValue Der neue Wert als Zahl zwischen 0 und 1023.
     */
    void valueChanged(AdcChannel channel, int newValue);
}
