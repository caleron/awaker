package com.awaker.automation.tasks;

import com.awaker.global.router.CommandRouter;
import com.awaker.gpio.LightChannel;
import com.awaker.gpio.LightCommand;
import com.awaker.server.json.CommandData;

/**
 * Task to trigger an SET_CHANNEL_BRIGHTNESS command.
 */
public class LightTask extends BaseTaskAction {

    private final LightChannel channel;
    private final int targetBrightness;
    private final int smoothDuration;

    public LightTask(int id, LightChannel channel, int targetBrightness, int smoothDuration) {
        super(id);
        this.channel = channel;
        this.targetBrightness = targetBrightness;
        this.smoothDuration = smoothDuration;
    }

    public LightTask(int id, LightChannel channel, int targetBrightness) {
        super(id);
        this.channel = channel;
        this.targetBrightness = targetBrightness;
        this.smoothDuration = -1;
    }

    @Override
    public void run() {
        CommandData data = new CommandData();
        data.brightness = targetBrightness;
        data.channel = channel;
        data.smooth = true;
        data.duration = smoothDuration;
        CommandRouter.handleCommand(LightCommand.SET_CHANNEL_BRIGHTNESS, data);
    }
}
