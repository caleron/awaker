package com.awaker.gpio;

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.math.BigDecimal;

public class PwmExtensionController {
    //min/max step count to use at 50Hz
    private static final int ABSOLUTE_MINIMUM = 155;
    private static final int ABSOLUTE_MAXIMUM = 650;

    //bounds to use
    private static final int MINIMUM_STEPS = 155;
    private static final int MAXIMUM_STEPS = 500;

    private static PwmExtensionController instance = null;
    private PCA9685GpioProvider provider;

    private PwmExtensionController() {
        try {
            //gibt wohl nur bus 1 auf raspberry
            //50 hertz setzen
            I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
            provider = new PCA9685GpioProvider(bus, 0x40, new BigDecimal("50"));
            provisionPwmOutputs(provider);

            // Reset outputs
            provider.reset();
        } catch (I2CFactory.UnsupportedBusNumberException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the servo position.
     * <p>
     * http://www.micropik.com/PDF/SG90Servo.pdf
     * at 50Hz Pwm Period = 20ms:
     * -90 = 1ms = 5% power
     * 0 = 1.5ms = 7.5%
     * 90 = 2ms bzw. 10%
     *
     * @param pin    the pin of the servo from {@link PCA9685Pin}
     * @param degree from -90 to +90
     */
    public void setServoPosition(Pin pin, short degree) {
        //middle step count
        int middleSteps = (int) ((MAXIMUM_STEPS + MINIMUM_STEPS) / 2.0);
        //scale between min and max
        int margin = MAXIMUM_STEPS - middleSteps;
        int offPosition = (int) (middleSteps + ((degree / 90.0) * margin));

        //ensure off position is in range
        offPosition = Math.max(MINIMUM_STEPS, Math.min(MAXIMUM_STEPS, offPosition));

        provider.setPwm(pin, 0, offPosition);
    }

    public static void init() {
        if (instance != null) {
            throw new RuntimeException("PwmExtensionController already initialized!");
        }
        instance = new PwmExtensionController();
    }

    private static GpioPinPwmOutput[] provisionPwmOutputs(final PCA9685GpioProvider gpioProvider) {
        GpioController gpio = GpioFactory.getInstance();
        GpioPinPwmOutput myOutputs[] = {
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_00, "Pulse 00"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_01, "Pulse 01"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_02, "Pulse 02"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_03, "Pulse 03"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_04, "Pulse 04"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_05, "Pulse 05")};
        return myOutputs;
    }
}
