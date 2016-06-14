package com.awaker.gpio;

import com.awaker.gpio.adc.AdcChannel;
import com.awaker.gpio.adc.AdcListener;
import com.awaker.gpio.adc.AdcObserver;
import com.awaker.gpio.adc.AnalogListener;

public class AnalogControls implements AdcListener {
    private AnalogListener listener;

    private Thread observerThread;
    private AdcObserver observer;

    private static final AdcChannel VOLUME_CHANNEL = AdcChannel.CH0;
    private static final AdcChannel WHITE_CHANNEL = AdcChannel.CH1;
    private static final AdcChannel RED_CHANNEL = AdcChannel.CH2;
    private static final AdcChannel GREEN_CHANNEL = AdcChannel.CH3;
    private static final AdcChannel BLUE_CHANNEL = AdcChannel.CH4;


    public AnalogControls(AnalogListener listener) {
        this.listener = listener;

        //Kanäle 0-4 überwachen
        observer = new AdcObserver(new AdcChannel[]{VOLUME_CHANNEL, WHITE_CHANNEL, RED_CHANNEL, GREEN_CHANNEL, BLUE_CHANNEL});

        observer.addListener(this);

        observer.start();
    }

    public void shutdown() {
        observer.stop();
    }

    @Override
    public void valueChanged(AdcChannel channel, int newValue) {
        newValue = (int) ((newValue / 1023f) * 100f);

        switch (channel) {
            case CH0:
                listener.setVolume(newValue);
                break;
            case CH1:
                listener.setWhite(newValue);
                break;
            case CH2:
                listener.setRed(newValue);
                break;
            case CH3:
                listener.setGreen(newValue);
                break;
            case CH4:
                listener.setBlue(newValue);
                break;
        }
    }
}
