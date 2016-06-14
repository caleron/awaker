package com.awaker.gpio.adc;

/**
 * Stellt einen Kanal des ADC's dar.
 */
public enum AdcChannel {
    CH0(0),
    CH1(1),
    CH2(2),
    CH3(3),
    CH4(4),
    CH5(5),
    CH6(6),
    CH7(7);

    private int channel;

    AdcChannel(int channel) {
        this.channel = channel;
    }

    public int getNumber() {
        return this.channel;
    }
}
