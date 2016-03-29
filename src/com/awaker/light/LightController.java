package com.awaker.light;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

import java.awt.*;

public class LightController {

    private int colorBrightness = 100;
    private int whiteBrightness = 0;

    private static final int PWM_PIN_RED = 1;
    private static final int PWM_PIN_GREEN = 4;
    private static final int PWM_PIN_BLUE = 5;
    private static final int PWM_PIN_WHITE = 6;

    private float red, green, blue;
    //Steht für die letzte über die Funktion updateColor gesetzte Farbe
    private Color currentColor;

    private String colorMode = "music";
    private Thread animationThread;

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
        startAnimationThread(this::doFadeColorLightsOut);
    }

    private void animateColorCircle() {
        startAnimationThread(this::doAnimateColor);
    }

    private void startAnimationThread(Runnable target) {
        //bestehenden Thread beenden
        while (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }

        animationThread = new Thread(target);
        animationThread.start();
    }

    private void cancelAnimation() {
        if (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();
        }
    }

    public void setWhiteBrightness(int brightness) {
        whiteBrightness = brightness;
        SoftPwm.softPwmWrite(PWM_PIN_WHITE, whiteBrightness);
    }

    public void updateColor(Color color, boolean smooth) {
        currentColor = color;
        if (smooth) {
            red = avg(red, ((color.getRed() / 255f) * colorBrightness));
            green = avg(green, ((color.getGreen() / 255f) * colorBrightness));
            blue = avg(blue, ((color.getBlue() / 255f) * colorBrightness));
        } else {
            red = (color.getRed() / 255f) * colorBrightness;
            green = (color.getGreen() / 255f) * colorBrightness;
            blue = (color.getBlue() / 255f) * colorBrightness;
        }
        refreshColorPins();
    }

    private void refreshColorPins() {
        SoftPwm.softPwmWrite(PWM_PIN_RED, (int) red);
        SoftPwm.softPwmWrite(PWM_PIN_GREEN, (int) green);
        SoftPwm.softPwmWrite(PWM_PIN_BLUE, (int) blue);
    }

    private void doFadeColorLightsOut() {
        while ((red > 0 || green > 0 || blue > 0) && !animationThread.isInterrupted()) {
            red = Math.max(red - 1, 0);
            green = Math.max(green - 1, 0);
            blue = Math.max(blue - 1, 0);

            refreshColorPins();

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    private void doAnimateColor() {
        float hue = 0f;

        while (!animationThread.isInterrupted()) {
            Color c = Color.getHSBColor(hue, 1, colorBrightness / 100f);

            red = c.getRed();
            green = c.getGreen();
            blue = c.getBlue();

            refreshColorPins();

            hue = hue + 0.01f;

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    public void setColorBrightness(int newValue) {
        if (newValue > 0 && newValue <= 100) {
            colorBrightness = newValue;
        }
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;

        switch (colorMode) {
            case "custom":
                cancelAnimation();
                //nichts zu tun
                break;
            case "colorCircle":
                animateColorCircle();
                break;
            default: //also music
                cancelAnimation();
                break;
        }
    }

    private static float avg(float a, float b) {
        return (a + b) / 2f;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder(100);

        sb.append("colorMode:");
        sb.append(colorMode).append(";");

        if (colorMode.equals("custom")) {
            sb.append("currentColor:");
            sb.append(currentColor.getRGB()).append(";");
        }

        sb.append("whiteBrightness:");
        sb.append(whiteBrightness).append(";");

        sb.append("colorBrightness:");
        sb.append(colorBrightness).append(";");

        return sb.toString();
    }
}
