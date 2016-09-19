package com.awaker.automation;

import com.awaker.audio_in.AudioCapture;
import com.awaker.gpio.LightChannel;
import com.awaker.gpio.LightController;

public class Automator implements EnvironmentEventListener {

    private final LightController lightController;

    public Automator(LightController lightController) {
        this.lightController = lightController;
        AutoLighter.start(this);
        new AudioCapture(this).start();
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
        if (lightController.getChannelBrightness(LightChannel.ALL) < 10) {
            lightController.setBrightness(LightChannel.WHITE, 40, 10000);
        }
    }

    @Override
    public void sunset() {
        if (lightController == null)
            return;
        if (lightController.getChannelBrightness(LightChannel.ALL) < 10) {
            lightController.setBrightness(LightChannel.WHITE, 50, 10000);
        }
    }
}
