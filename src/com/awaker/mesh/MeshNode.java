package com.awaker.mesh;

import com.awaker.audio.AudioCommand;
import com.awaker.global.router.Command;
import com.awaker.global.router.CommandRouter;
import com.awaker.gpio.LightCommand;
import com.awaker.server.json.CommandData;

import static com.awaker.audio.AudioCommand.*;
import static com.awaker.gpio.LightCommand.*;

public enum MeshNode {
    NODE1(10,
            new Command[]{
                    SET_VOLUME,
                    SET_WHITE_BRIGHTNESS,
                    SET_ANIMATION_BRIGHTNESS,
                    SET_RED_BRIGHTNESS,
                    SET_GREEN_BRIGHTNESS,
                    SET_BLUE_BRIGHTNESS
            },
            new Command[]{
                    PLAY_NEXT,
                    TOGGLE_PLAY_PAUSE,
                    PLAY_PREVIOUS,
                    FADE_LIGHTS_OUT,
                    SWITCH_OFF_LIGHTS,
                    SWITCH_ON_WHITE_LIGHT,
                    NEXT_COLOR_MODE
            });

    private int nodeId;
    private Command[] analogControlCommands;
    private Command[] buttonCommands;

    MeshNode(int nodeId, Command[] analogControlCommands, Command[] buttonCommands) {
        this.nodeId = nodeId;
        this.analogControlCommands = analogControlCommands;
        this.buttonCommands = buttonCommands;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void handleAnalogChange(int index, int value) {
        if (index >= analogControlCommands.length)
            return;

        CommandData data = new CommandData();
        Command command = analogControlCommands[index];
        value = (int) (value / 2.55f);

        if (command instanceof LightCommand) {
            data.brightness = value;
        } else if (command instanceof AudioCommand && command.equals(SET_VOLUME)) {
            data.volume = value;
        }
        data.smooth = false;

        CommandRouter.handleCommand(command, data);
    }

    public void handleButtonPressed(int index) {
        if (index >= buttonCommands.length)
            return;

        CommandRouter.handleCommand(buttonCommands[index]);
    }

    public static MeshNode getNodeForId(int nodeId) {
        MeshNode[] nodes = MeshNode.values();
        for (MeshNode node : nodes) {
            if (node.getNodeId() == nodeId) {
                return node;
            }
        }
        return null;
    }
}
