package com.awaker.light;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

import java.awt.*;

public class LightController {

    private int maxBrightness = 100;

    static final int PWM_PIN_RED = 1;
    static final int PWM_PIN_GREEN = 4;
    static final int PWM_PIN_BLUE = 5;

    int red, green, blue;
    private boolean interrupt = false;

    public LightController() {
        // initialize wiringPi library
        Gpio.wiringPiSetup();

        // create soft-pwm pins (min=0 ; max=100)
        SoftPwm.softPwmCreate(PWM_PIN_RED, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_GREEN, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_BLUE, 0, 100);

        //new Thread(this::fadeLightsOut).start();
        System.out.println("Lightcontroller initialized");
    }

    public void updateColor(Color color) {
        red = (int) (color.getRed() / 255.0) * maxBrightness;
        green = (int) (color.getGreen() / 255.0) * maxBrightness;
        blue = (int) (color.getBlue() / 255.0) * maxBrightness;

        refreshPins();
    }

    private void refreshPins() {
        SoftPwm.softPwmWrite(PWM_PIN_RED, red);
        SoftPwm.softPwmWrite(PWM_PIN_GREEN, green);
        SoftPwm.softPwmWrite(PWM_PIN_BLUE, blue);
    }

    private void fadeLightsOut() {
        while (!interrupt) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            red = Math.max(red - 3, 0);
            green = Math.max(green - 3, 0);
            blue = Math.max(blue - 3, 0);

            refreshPins();
        }
    }

    public void setMaxBrightness(int newValue) {
        if (newValue > 0 && newValue <= 100) {
            maxBrightness = newValue;
        }
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

}
