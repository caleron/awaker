package com.awaker.gpio;

import com.awaker.server.json.Answer;
import com.awaker.util.Log;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

import java.awt.*;

public class LightController {

    private int animationBrightness = 100;
    private int whiteBrightness = 0;

    private static final int PWM_PIN_RED = 9;
    private static final int PWM_PIN_GREEN = 7;
    private static final int PWM_PIN_BLUE = 0;
    private static final int PWM_PIN_WHITE = 8;

    private float red, green, blue;
    //Steht für die letzte über die Funktion updateColor gesetzte Farbe
    private Color currentColor = Color.BLACK;

    private String colorMode = "music";
    private Thread animationThread;

    public LightController() {
        //wiringpi library initialisieren
        //Gpio.wiringPiSetup();

        /**
         * Soft-PWM-Pins erstellen mit möglichen Werten zwischen 0 und 100. Der interne Taktzyklus ist 100µS lang.
         * Durch eine Auflösung bis 100 ergibt dies 100 Taktzyklen pro PWM-Takt, also insgesamt 100 * 100µS = 10ms pro
         * PWM-Takt, was eine PWM-Frequenz von 100Hz bedeutet.
         */
        SoftPwm.softPwmCreate(PWM_PIN_RED, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_GREEN, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_BLUE, 0, 100);
        SoftPwm.softPwmCreate(PWM_PIN_WHITE, 0, 100);

        Log.message("Lightcontroller initialisiert");
    }

    /**
     * Startet die langsame Abdunkelung der Farb-LEDs
     */
    public void fadeOutColorLights() {
        startAnimationThread(this::doFadeColorLightsOut);
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

    /**
     * Setzt die Helligkeit der weißen LED
     *
     * @param brightness Helligkeit zwischen 0 und 100
     */
    public void setWhiteBrightness(int brightness) {
        //wert zwischen 0 und 100 sicherstellen
        whiteBrightness = Math.max(0, Math.min(100, brightness));

        SoftPwm.softPwmWrite(PWM_PIN_WHITE, whiteBrightness);
    }

    /**
     * Setzt die Helligkeit der roten LED
     *
     * @param brightness Helligkeit zwischen 0 und 100
     */
    public void setRedBrightness(int brightness) {
        //wert zwischen 0 und 100 sicherstellen
        brightness = Math.max(0, Math.min(100, brightness));
        red = brightness;
        currentColor = new Color((int) Math.min(brightness * 2.55f, 255), currentColor.getGreen(), currentColor.getBlue());

        SoftPwm.softPwmWrite(PWM_PIN_RED, brightness);
    }

    /**
     * Setzt die Helligkeit der grünen LED
     *
     * @param brightness Helligkeit zwischen 0 und 100
     */
    public void setGreenBrightness(int brightness) {
        //wert zwischen 0 und 100 sicherstellen
        brightness = Math.max(0, Math.min(100, brightness));
        green = brightness;
        currentColor = new Color(currentColor.getRed(), ((int) Math.min(brightness * 2.55f, 255)), currentColor.getBlue());

        SoftPwm.softPwmWrite(PWM_PIN_GREEN, brightness);
    }

    /**
     * Setzt die Helligkeit der blauen LED
     *
     * @param brightness Helligkeit zwischen 0 und 100
     */
    public void setBlueBrightness(int brightness) {
        //wert zwischen 0 und 100 sicherstellen
        brightness = Math.max(0, Math.min(100, brightness));
        blue = brightness;
        currentColor = new Color(currentColor.getRed(), currentColor.getGreen(), ((int) Math.min(brightness * 2.55f, 255)));

        SoftPwm.softPwmWrite(PWM_PIN_BLUE, brightness);
    }

    /**
     * Setzt die Farbhelligkeit
     *
     * @param newValue Farbhelligkeit als Wert zwischen 0 und 100
     */
    public void setAnimationBrightness(int newValue) {
        //wert zwischen 0 und 100 sicherstellen
        animationBrightness = Math.max(0, Math.min(100, newValue));

        animationBrightness = newValue;
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
            red = getNewColor(red, ((color.getRed() / 255f) * animationBrightness));
            green = getNewColor(green, ((color.getGreen() / 255f) * animationBrightness));
            blue = getNewColor(blue, ((color.getBlue() / 255f) * animationBrightness));
        } else {
            red = (color.getRed() / 255f) * 100;
            green = (color.getGreen() / 255f) * 100;
            blue = (color.getBlue() / 255f) * 100;
        }
        refreshColorPins();
    }

    /**
     * Schreibt die Farbwerte zu den PINs
     */
    private void refreshColorPins() {
        SoftPwm.softPwmWrite(PWM_PIN_RED, (int) red);
        SoftPwm.softPwmWrite(PWM_PIN_GREEN, (int) green);
        SoftPwm.softPwmWrite(PWM_PIN_BLUE, (int) blue);
    }

    /**
     * Dunkelt die Farb-LEDs langsam über einen Zeitraum von bis zu 5s ab
     */
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

    /**
     * Führt die Farbkreisanimation mit einer Zyklusdauer von 12.5s durch
     */
    private void doAnimateColor() {
        float hue = 0f;

        while (!animationThread.isInterrupted()) {
            Color c = Color.getHSBColor(hue, 1, animationBrightness / 100f);

            red = c.getRed();
            green = c.getGreen();
            blue = c.getBlue();

            refreshColorPins();

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
    private static float getNewColor(float oldColor, float newColor) {
        if (oldColor > newColor) {
            return (oldColor + newColor) / 2f;
        } else {
            //gewichteter Durchschnitt
            return (oldColor * 2f + newColor) / 3f;
        }
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
        answer.whiteBrightness = whiteBrightness;

        answer.animationBrightness = animationBrightness;

        return answer;
    }
}
