package com.awaker.gpio;

import com.awaker.util.Log;
import com.pi4j.gpio.extension.mcp.MCP3008GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP3008Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.spi.SpiChannel;

import java.io.IOException;

public class AnalogControls implements GpioPinListenerAnalog {
    private AnalogListener listener;

    private static final Pin VOLUME_CHANNEL = MCP3008Pin.CH0;
    private static final Pin WHITE_CHANNEL = MCP3008Pin.CH1;
    private static final Pin RED_CHANNEL = MCP3008Pin.CH2;
    private static final Pin GREEN_CHANNEL = MCP3008Pin.CH3;
    private static final Pin BLUE_CHANNEL = MCP3008Pin.CH4;

    private GpioPinAnalogInput pin_volume;
    private GpioPinAnalogInput pin_white;
    private GpioPinAnalogInput pin_red;
    private GpioPinAnalogInput pin_green;
    private GpioPinAnalogInput pin_blue;


    public AnalogControls(AnalogListener listener) {
        this.listener = listener;

        MCP3008GpioProvider provider = null;

        try {
            provider = new MCP3008GpioProvider(SpiChannel.CS0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (provider == null) {
            Log.error("Could not open MCP3008 Provider");
            return;
        }

        GpioController gpioController = GpioFactory.getInstance();

        pin_volume = gpioController.provisionAnalogInputPin(provider, VOLUME_CHANNEL, "pin_volume");
        pin_white = gpioController.provisionAnalogInputPin(provider, WHITE_CHANNEL, "pin_white");
        pin_red = gpioController.provisionAnalogInputPin(provider, RED_CHANNEL, "pin_red");
        pin_green = gpioController.provisionAnalogInputPin(provider, GREEN_CHANNEL, "pin_green");
        pin_blue = gpioController.provisionAnalogInputPin(provider, BLUE_CHANNEL, "pin_blue");

        pin_volume.addListener(this);
        pin_white.addListener(this);
        pin_red.addListener(this);
        pin_green.addListener(this);
        pin_blue.addListener(this);
    }

    @Override
    public void handleGpioPinAnalogValueChangeEvent(GpioPinAnalogValueChangeEvent event) {
        int newValue = (int) ((event.getValue() / 1023.0) * 100.0);

        if (pin_volume.equals(event.getPin())) {
            listener.setVolume(newValue);

        } else if (pin_white.equals(event.getPin())) {
            listener.setWhiteBrightness(newValue);

        } else if (pin_red.equals(event.getPin())) {
            listener.setRed(newValue);

        } else if (pin_green.equals(event.getPin())) {
            listener.setGreen(newValue);

        } else if (pin_blue.equals(event.getPin())) {
            listener.setBlue(newValue);

        } else {
            Log.message("received event from unknown pin: " + event.getPin().getName());
        }
    }
}
