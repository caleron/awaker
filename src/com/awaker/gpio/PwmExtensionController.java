package com.awaker.gpio;

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.math.BigDecimal;

public class PwmExtensionController {
    private static PwmExtensionController instance = null;
    private PCA9685GpioProvider provider;

    private PwmExtensionController() {
        try {
            //gibt wohl nur bus 1 auf raspberry
            //50 hertz setzen
            provider = new PCA9685GpioProvider(I2CFactory.getInstance(I2CBus.BUS_1), 1, new BigDecimal(50));
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
        double min = 0.05;
        double max = 0.1;
        //now scale between min and max
        double power = (min + 0.025) + (degree / 90.0) * 0.025;
        //12 bit pwm = 4096 steps = 0 ... 4095 as pwm value
        double steps = 4095;
        //thats the step where the pin should turn off, between 0 and 4095 inclusively
        long offPosition = Math.round(power * steps);
        //cap for security
        offPosition = (long) Math.max(Math.min(steps, offPosition), 0);
        //now with onPosition = 0, the pwm on to off ratio equals to the factor
        provider.setPwm(pin, 0, (int) offPosition);
    }

    public static void init() {
        if (instance != null) {
            throw new RuntimeException("PwmExtensionController already initialized!");
        }
        instance = new PwmExtensionController();
    }

}
