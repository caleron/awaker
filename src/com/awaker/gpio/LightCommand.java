package com.awaker.gpio;

import com.awaker.global.Command;

public enum LightCommand implements Command {
    //Licht
    /**
     * Setzt die Helligkeit des weißen Kanals auf <code>brightness</code>. <code>smooth</code> optional.
     */
    SET_WHITE_BRIGHTNESS("setWhiteBrightness"),
    /**
     * Setzt die Helligkeit des Animationen auf <code>brightness</code>. <code>smooth</code> optional.
     */
    SET_ANIMATION_BRIGHTNESS("setAnimationBrightness"),
    /**
     * Setzt die Helligkeit des roten Kanals auf <code>brightness</code>. <code>smooth</code> optional.
     */
    SET_RED_BRIGHTNESS("setRedBrightness"),
    /**
     * Setzt die Helligkeit des grünen Kanals auf <code>brightness</code>. <code>smooth</code> optional.
     */
    SET_GREEN_BRIGHTNESS("setGreenBrightness"),
    /**
     * Setzt die Helligkeit des blauen Kanals auf <code>brightness</code>. <code>smooth</code> optional.
     */
    SET_BLUE_BRIGHTNESS("setBlueBrightness"),
    /**
     * Fadet langsam das Licht aus. Keine Parameter.
     */
    FADE_LIGHTS_OUT("fadeLightsOut"),
    /**
     * Schaltet das weiße Licht an. Keine Parameter.
     */
    SWITCH_ON_WHITE_LIGHT("switchOnWhiteLight"),
    /**
     * Schaltet das Licht aus.
     */
    SWITCH_OFF_LIGHTS("switchOffLights"),

    /**
     * Setzt den Farbmodus auf <code>colorMode</code>
     */
    SET_COLOR_MODE("setColorMode"),
    /**
     * Setzt die Farbe auf <code>color</code> als rgb-int.
     */
    SET_COLOR("setColor"),
    /**
     * Setzt die Farbe auf <code>green</code>, <code>red</code> und <code>blue</code>
     */
    SET_RGBCOLOR("setRGBColor"),
    /**
     * Setzt Visualisationsmethode. Nicht unterstützt.
     */
    CHANGE_VISUALIZATION("changeVisualization");

    private final String action;

    LightCommand(String action) {
        this.action = action;
    }

    @Override
    public String getAction() {
        return action;
    }
}
