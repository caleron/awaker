package com.awaker.gpio;

import com.awaker.global.Command;

public enum LightCommand implements Command {
    //Licht
    SET_WHITE_BRIGHTNESS("setWhiteBrightness"),
    SET_ANIMATION_BRIGHTNESS("setAnimationBrightness"),
    SET_RED_BRIGHTNESS("setRedBrightness"),
    SET_GREEN_BRIGHTNESS("setGreenBrightness"),
    SET_BLUE_BRIGHTNESS("setBlueBrightness"),

    SET_COLOR_MODE("setColorMode"),
    SET_COLOR("setColor"),
    SET_RGBCOLOR("setRGBColor"),
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
