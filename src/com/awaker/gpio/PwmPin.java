package com.awaker.gpio;

import com.pi4j.wiringpi.SoftPwm;

class PwmPin {
    private final int pin;
    private int value;

    private Thread smoothThread;

    PwmPin(int pin) {
        this.pin = pin;
        SoftPwm.softPwmCreate(pin, 0, 100);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        //wert zwischen 0 und 100 sicherstellen
        value = Math.max(0, Math.min(100, value));

        this.value = value;
        SoftPwm.softPwmWrite(pin, value);
    }

    void setValue(int value, Boolean smooth) {
        //wert zwischen 0 und 100 sicherstellen
        value = Math.max(0, Math.min(100, value));

        if (smooth) {
            int duration = Math.abs(this.value - value) * 10;
            setValue(value, duration);
        } else {
            setValue(value);
        }
    }

    void setValue(int value, int duration) {
        //wert zwischen 0 und 100 sicherstellen
        value = Math.max(0, Math.min(100, value));

        if (this.value == value)
            return;

        final int direction = this.value > value ? -1 : 1;
        final int steps = Math.abs(this.value - value);
        final int sleepTime = duration / steps;

        if (smoothThread != null) {
            smoothThread.interrupt();
        }
        smoothThread = new Thread(() -> {
            for (int i = 0; i < steps; i++) {
                setValue(getValue() + direction);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        smoothThread.start();
    }

}
