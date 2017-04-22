package com.awaker.gpio;

import com.awaker.audio.PlayerMaster;
import com.awaker.global.router.*;
import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;
import com.awaker.util.Log;
import com.pi4j.wiringpi.Gpio;

import java.awt.*;

public class LightController implements CommandHandler, EventReceiver {
    private static LightController instance = null;

    private int animationBrightness = 100;

    private static final int PWM_PIN_RED = 9;
    private static final int PWM_PIN_GREEN = 7;
    private static final int PWM_PIN_BLUE = 0;
    private static final int PWM_PIN_WHITE = 8;

    private final PwmPin white, red, green, blue;
    //Steht für die letzte über die Funktion updateColor gesetzte Farbe
    private Color currentColor = Color.BLACK;

    private String colorMode = "music";
    private Thread animationThread;

    public LightController() {
        //Nur eine Instanz erlauben
        if (instance != null) {
            throw new RuntimeException("LightController already existing");
        }
        instance = this;
        //wiringpi library initialisieren
        Gpio.wiringPiSetup();

        /*
          Soft-PWM-Pins erstellen mit möglichen Werten zwischen 0 und 100. Der interne Taktzyklus ist 100µS lang.
          Durch eine Auflösung bis 100 ergibt dies 100 Taktzyklen pro PWM-Takt, also insgesamt 100 * 100µS = 10ms pro
          PWM-Takt, was eine PWM-Frequenz von 100Hz bedeutet.
         */
        white = new PwmPin(PWM_PIN_WHITE);
        red = new PwmPin(PWM_PIN_RED);
        blue = new PwmPin(PWM_PIN_BLUE);
        green = new PwmPin(PWM_PIN_GREEN);

        CommandRouter.registerHandler(LightCommand.class, this);
        EventRouter.registerReceiver(this, GlobalEvent.PLAYBACK_PAUSED);
        Log.message("Lightcontroller initialisiert");
    }

    public static LightController getInstance() {
        return instance;
    }

    @Override
    public Answer handleCommand(Command command, CommandData data, boolean buildAnswer) {
        if (!(command instanceof LightCommand)) {
            throw new RuntimeException("Received Wrong Command");
        }

        LightCommand cmd = (LightCommand) command;
        switch (cmd) {
            case CHANGE_VISUALIZATION:
                break;
            case SET_ANIMATION_BRIGHTNESS:
                setAnimationBrightness(data.brightness, data.smooth);
                break;
            case SET_COLOR:
                Color color = new Color(data.color, false);
                if (data.smooth) {
                    updateColorSmooth(color);
                } else {
                    updateColor(color, false);
                }
                break;
            case SET_COLOR_MODE:
                setColorMode(data.colorMode);
                PlayerMaster.getInstance().setColorMode(!data.colorMode.equals("music"));
                break;
            case NEXT_COLOR_MODE:
                nextColorMode();
                PlayerMaster.getInstance().setColorMode(!this.colorMode.equals("music"));
                break;
            case SET_RGBCOLOR:
                color = new Color(data.red, data.green, data.blue);
                if (data.smooth) {
                    updateColorSmooth(color);
                } else {
                    updateColor(color, false);
                }
                break;
            case SET_WHITE_BRIGHTNESS:
                setBrightness(LightChannel.WHITE, data.brightness, data.smooth);
                break;
            case SET_BLUE_BRIGHTNESS:
                setBrightness(LightChannel.BLUE, data.brightness, data.smooth);
                break;
            case SET_GREEN_BRIGHTNESS:
                setBrightness(LightChannel.GREEN, data.brightness, data.smooth);
                break;
            case SET_RED_BRIGHTNESS:
                setBrightness(LightChannel.RED, data.brightness, data.smooth);
                break;
            case SWITCH_OFF_LIGHTS:
                setBrightness(LightChannel.ALL, 0, 1000);
                break;
            case FADE_LIGHTS_OUT:
                setBrightness(LightChannel.ALL, 0, 30000);
                break;
            case SWITCH_ON_WHITE_LIGHT:
                setBrightness(LightChannel.WHITE, 70, 1000);
                break;
            case SET_CHANNEL_BRIGHTNESS:
                if (data.duration != -1) {
                    setBrightness(data.channel, data.brightness, data.duration);
                } else {
                    setBrightness(data.channel, data.brightness, data.smooth);
                }
                break;
        }
        if (buildAnswer) {
            return Answer.status();
        }
        return null;
    }

    @Override
    public void receiveGlobalEvent(GlobalEvent globalEvent) {
        switch (globalEvent) {
            case PLAYBACK_PAUSED:
                fadeOutColorLights();
        }
    }

    /**
     * Startet die langsame Abdunkelung der Farb-LEDs
     */
    private void fadeOutColorLights() {
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

    @SuppressWarnings("Duplicates")
    public void setBrightness(LightChannel channel, int value, Boolean smooth) {
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

    /**
     * @param channel  Der Lichtkanal
     * @param value    Wert zwischen 0 und 100
     * @param duration Dauer in ms
     */
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
    private void setAnimationBrightness(int newValue, boolean smooth) {
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
    private void updateColorSmooth(Color color) {
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
     * Wechselt in den nächsten Farbmodus aus der Reihe custom, music, colorCircle.
     */
    private void nextColorMode() {
        switch (this.colorMode) {
            case "custom":
                setColorMode("music");
                break;
            case "colorCircle":
                setColorMode("custom");
                break;
            default: //also music
                setColorMode("colorCircle");
                break;
        }
    }

    /**
     * Setzt den Farbmodus. Mögliche Werte: custom, music, colorCircle
     *
     * @param colorMode Der Farbmodus
     */
    private void setColorMode(String colorMode) {
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
    public Answer writeStatus(Answer answer) {
        answer.colorMode = colorMode;

        if (colorMode.equals("custom")) {
            answer.currentColor = currentColor.getRGB();
        }
        answer.whiteBrightness = white.getValue();

        answer.animationBrightness = animationBrightness;

        return answer;
    }
}
