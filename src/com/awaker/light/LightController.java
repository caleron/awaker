package com.awaker.light;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

import java.awt.*;

public class LightController {

    private int colorBrightness = 50;
    private int whiteBrightness = 0;

    private static final int PWM_PIN_RED = 1;
    private static final int PWM_PIN_GREEN = 4;
    private static final int PWM_PIN_BLUE = 5;
    private static final int PWM_PIN_WHITE = 6;

    private int red, green, blue;

    public LightController() {
        // initialize wiringPi library
        Gpio.wiringPiSetup();

        // create soft-pwm pins (min=0 ; max=100)
        SoftPwm.softPwmCreate(PWM_PIN_RED, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_GREEN, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_BLUE, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_WHITE, 0, 100);

        System.out.println("Lightcontroller initialized");
    }

    public void fadeOutColorLights() {
        new Thread(this::fadeColorLightsOut).start();
    }

    public int getWhiteBrightness() {
        return whiteBrightness;
    }

    public void setWhiteBrightness(int brightness) {
        whiteBrightness = brightness;
        SoftPwm.softPwmWrite(PWM_PIN_WHITE, whiteBrightness);
    }

    public void updateColor(Color color) {
        red = (int) ((color.getRed() / 255.0) * colorBrightness);
        green = (int) ((color.getGreen() / 255.0) * colorBrightness);
        blue = (int) ((color.getBlue() / 255.0) * colorBrightness);

        refreshColorPins();
    }

    private void refreshColorPins() {
        SoftPwm.softPwmWrite(PWM_PIN_RED, red);
        SoftPwm.softPwmWrite(PWM_PIN_GREEN, green);
        SoftPwm.softPwmWrite(PWM_PIN_BLUE, blue);
    }

    private void fadeColorLightsOut() {
        while (red > 0 || green > 0 || blue > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            red = Math.max(red - 1, 0);
            green = Math.max(green - 1, 0);
            blue = Math.max(blue - 1, 0);

            refreshColorPins();
        }
    }

    public void setColorBrightness(int newValue) {
        if (newValue > 0 && newValue <= 100) {
            colorBrightness = newValue;
        }
    }
}
