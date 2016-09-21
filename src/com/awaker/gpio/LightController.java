package com.awaker.gpio;

import com.awaker.server.json.Answer;
import com.awaker.util.Log;

import java.awt.*;

public class LightController {

    private int animationBrightness = 100;

    private static final int PWM_PIN_RED = 9;
    private static final int PWM_PIN_GREEN = 7;
    private static final int PWM_PIN_BLUE = 0;
    private static final int PWM_PIN_WHITE = 8;

    private PwmPin white, red, green, blue;
    //Steht für die letzte über die Funktion updateColor gesetzte Farbe
    private Color currentColor = Color.BLACK;

    private String colorMode = "music";
    private Thread animationThread;

    public LightController() {
        //wiringpi library initialisieren
        //Gpio.wiringPiSetup();

        /*
          Soft-PWM-Pins erstellen mit möglichen Werten zwischen 0 und 100. Der interne Taktzyklus ist 100µS lang.
          Durch eine Auflösung bis 100 ergibt dies 100 Taktzyklen pro PWM-Takt, also insgesamt 100 * 100µS = 10ms pro
          PWM-Takt, was eine PWM-Frequenz von 100Hz bedeutet.
         */
        white = new PwmPin(PWM_PIN_WHITE);
        red = new PwmPin(PWM_PIN_RED);
        blue = new PwmPin(PWM_PIN_BLUE);
        green = new PwmPin(PWM_PIN_GREEN);

        Log.message("Lightcontroller initialisiert");
    }

    /**
     * Startet die langsame Abdunkelung der Farb-LEDs
     */
    public void fadeOutColorLights() {
        setBrightness(LightChannel.COLORS, 0, true);
    }

    /**
     * Startet die Farbkreisanimation
     */
    private void animateColorCircle() {
        startAnimationThread(this::doAnimateColor);
    }

    /**
     * Startet eine Animation im Animationsthread. Dabei wird vorher sichergestellt, dass keine andere Animation läuft.
     *
     * @param target Die Animationsmethode
     */
    private void startAnimationThread(Runnable target) {
        //bestehenden Thread mit absoluter Sicherheit beenden
        while (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();
            //Immer wieder interrupt aufrufen, bis der Thread beendet ist
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        //neuen Thread starten
        animationThread = new Thread(target);
        animationThread.start();
    }

    /**
     * Bricht eine Animation asynchron ab.
     */
    private void cancelAnimation() {
        if (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();
        }
    }

    public void setBrightness(LightChannel channel, int value) {
        setBrightness(channel, value, false);
    }

    @SuppressWarnings("Duplicates")
    public void setBrightness(LightChannel channel, int value, boolean smooth) {
        switch (channel) {
            case WHITE:
                white.setValue(value, smooth);
                break;
            case RED:
                red.setValue(value, smooth);
                break;
            case BLUE:
                blue.setValue(value, smooth);
                break;
            case GREEN:
                green.setValue(value, smooth);
                break;
            case ALL:
                white.setValue(value, smooth);
                red.setValue(value, smooth);
                blue.setValue(value, smooth);
                green.setValue(value, smooth);
                break;
            case COLORS:
                red.setValue(value, smooth);
                green.setValue(value, smooth);
                blue.setValue(value, smooth);
                break;
            case ANIMATION:
                animationBrightness = value;
                break;
        }
    }

    public int getChannelBrightness(LightChannel channel) {
        switch (channel) {
            case WHITE:
                return white.getValue();
            case RED:
                return red.getValue();
            case BLUE:
                return blue.getValue();
            case GREEN:
                return green.getValue();
            case ALL:
                return Math.max(white.getValue(), Math.max(red.getValue(), Math.max(blue.getValue(), green.getValue())));
            case COLORS:
                return Math.max(red.getValue(), Math.max(blue.getValue(), green.getValue()));
            case ANIMATION:
                return animationBrightness;
            default:
                return 0;
        }
    }

    @SuppressWarnings("Duplicates")
    public void setBrightness(LightChannel channel, int value, int duration) {
        switch (channel) {
            case WHITE:
                white.setValue(value, duration);
                break;
            case RED:
                red.setValue(value, duration);
                break;
            case BLUE:
                blue.setValue(value, duration);
                break;
            case GREEN:
                green.setValue(value, duration);
                break;
            case ALL:
                white.setValue(value, duration);
                red.setValue(value, duration);
                blue.setValue(value, duration);
                green.setValue(value, duration);
                break;
            case COLORS:
                white.setValue(value, duration);
                red.setValue(value, duration);
                blue.setValue(value, duration);
                break;
            case ANIMATION:
                animationBrightness = value;
                break;
        }
    }

    /**
     * Setzt die Farbhelligkeit
     *
     * @param newValue Farbhelligkeit als Wert zwischen 0 und 100
     */
    public void setAnimationBrightness(int newValue, boolean smooth) {
        //wert zwischen 0 und 100 sicherstellen
        if (smooth) {
            new Thread(() -> {
                int steps = Math.abs(newValue - animationBrightness);
                int stepSize = newValue > animationBrightness ? 1 : -1;
                for (int i = 0; i < steps; i++) {
                    animationBrightness += stepSize;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                    if (animationBrightness >= 100 || animationBrightness <= 0) {
                        animationBrightness = Math.max(0, Math.min(100, newValue));
                        break;
                    }
                }
            }).start();
        } else {
            animationBrightness = Math.max(0, Math.min(100, newValue));
        }
    }

    /**
     * Setzt eine neue Farbe.
     *
     * @param color   Die Farbe
     * @param isMusic True, wenn der Farbübergang geglättet werden soll.
     */
    public void updateColor(Color color, boolean isMusic) {
        currentColor = color;
        if (isMusic) {
            red.setValue(getNewColor(red.getValue(), (int) ((color.getRed() / 255f) * animationBrightness)));
            green.setValue(getNewColor(green.getValue(), (int) ((color.getGreen() / 255f) * animationBrightness)));
            blue.setValue(getNewColor(blue.getValue(), (int) ((color.getBlue() / 255f) * animationBrightness)));
        } else {
            red.setValue((int) ((color.getRed() / 255f) * 100));
            green.setValue((int) ((color.getGreen() / 255f) * 100));
            blue.setValue((int) ((color.getBlue() / 255f) * 100));
        }
    }

    /**
     * Setzt eine neue Farbe mit sanftem Übergang
     *
     * @param color Die Farbe
     */
    public void updateColorSmooth(Color color) {
        currentColor = color;
        red.setValue((int) ((color.getRed() / 255f) * 100), true);
        green.setValue((int) ((color.getGreen() / 255f) * 100), true);
        blue.setValue((int) ((color.getBlue() / 255f) * 100), true);
    }

    /**
     * Führt die Farbkreisanimation mit einer Zyklusdauer von 12.5s durch
     */
    private void doAnimateColor() {
        float hue = 0f;

        while (!animationThread.isInterrupted()) {
            Color c = Color.getHSBColor(hue, 1, animationBrightness / 100f);

            red.setValue(c.getRed());
            green.setValue(c.getGreen());
            blue.setValue(c.getBlue());

            hue = hue + 0.004f;

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    /**
     * Setzt den Farbmodus. Mögliche Werte: custom, music, colorCircle
     *
     * @param colorMode Der Farbmodus
     */
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

    /**
     * Berechnet einen neuen Farbwert aus oldColor und newColor
     *
     * @param oldColor alter Wert
     * @param newColor neuer Wert
     * @return neu angepasster Farbwert
     */
    private static int getNewColor(int oldColor, int newColor) {
        if (oldColor > newColor) {
            return (int) ((oldColor + newColor) / 2f);
        } else {
            //gewichteter Durchschnitt
            return (int) ((oldColor * 2f + newColor) / 3f);
        }
    }

    public boolean isLightOn() {
        return white.getValue() > 0 || red.getValue() > 0 || blue.getValue() > 0 || green.getValue() > 0;
    }

    /**
     * Schreibt den Status in das angegebene Answer-Objekt
     *
     * @param answer Das Answer-Objekt
     * @return das Answer-Objekt
     */
    public Answer getStatus(Answer answer) {
        answer.colorMode = colorMode;

        if (colorMode.equals("custom")) {
            answer.currentColor = currentColor.getRGB();
        }
        answer.whiteBrightness = white.getValue();

        answer.animationBrightness = animationBrightness;

        return answer;
    }
}
