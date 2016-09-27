package com.awaker.automation;

import com.awaker.audio_in.AudioCapture;
import com.awaker.gpio.LightChannel;
import com.awaker.gpio.LightController;
import com.awaker.util.Log;

public class Automator implements EnvironmentEventListener {

    private final LightController lightController;

    public Automator(LightController lightController) {
        this.lightController = lightController;
        AutoLighter.start(this);
        AudioCapture.start(this);
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
            lightController.setBrightness(LightChannel.WHITE, 40, 30000);
        }
    }

    @Override
    public void sunset() {
        if (lightController == null)
            return;

        Log.message("Sun is setting! Lighting if necessary...");

        if (lightController.getChannelBrightness(LightChannel.ALL) < 20) {
            lightController.setBrightness(LightChannel.WHITE, 50, 30000);
        }
    }
}
