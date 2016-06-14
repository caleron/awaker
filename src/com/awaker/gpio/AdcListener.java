package com.awaker.gpio;

public interface AdcListener {
    void valueChanged(AdcChannel channel, int newValue);
}
