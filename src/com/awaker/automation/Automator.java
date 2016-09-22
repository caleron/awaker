package com.awaker.automation;

import com.awaker.audio_in.AudioCapture;
import com.awaker.config.Config;
import com.awaker.config.ConfigKey;
import com.awaker.gpio.LightChannel;
import com.awaker.gpio.LightController;
import com.awaker.util.Log;

public class Automator implements EnvironmentEventListener {

    private final LightController lightController;

    public Automator(LightController lightController) {
        this.lightController = lightController;
        if (Config.getBool(ConfigKey.LIGHT_ON_SUNRISE, true) || Config.getBool(ConfigKey.LIGHT_ON_SUNSET, true)) {
            AutoLighter.start(this);
        }
        if (Config.getBool(ConfigKey.DETECT_CLAPS, false)) {
            new AudioCapture(this).start();
        }
    }

    @Override
    public void clapDetected() {
        if (lightController == null)
            return;

        if (lightController.isLightOn()) {
            lightController.setBrightness(LightChannel.ALL, 0, true);
        } else {
            lightController.setBrightness(LightChannel.WHITE, 40, true);
        }
    }

    @Override
    public void sunrise() {
        if (lightController == null)
            return;

        Log.message("Sun is rising! Lighting if necessary...");

        if (lightController.getChannelBrightness(LightChannel.ALL) < 10) {
            lightController.setBrightness(LightChannel.WHITE, 40, 10000);
        }
    }

    @Override
    public void sunset() {
        if (lightController == null)
            return;

        Log.message("Sun is setting! Lighting if necessary...");

        if (lightController.getChannelBrightness(LightChannel.ALL) < 10) {
            lightController.setBrightness(LightChannel.WHITE, 50, 10000);
        }
    }
}
