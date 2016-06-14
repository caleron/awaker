package com.awaker.gpio;

import com.pi4j.io.gpio.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Read an Analog to Digital Converter
 */
public class AdcObserver {
    private final static boolean DISPLAY_DIGIT = false;

    // 23: DOUT on the ADC is IN on the GPIO. ADC:Slave, GPIO:Master
    // 24: DIN on the ADC, OUT on the GPIO. Same reason as above.
    // SPI: Serial Peripheral Interface
    private static Pin spiClk = RaspiPin.GPIO_06; // clock
    private static Pin spiMiso = RaspiPin.GPIO_05; // mit D_out verbinden  MISO: Master In Slave Out
    private static Pin spiMosi = RaspiPin.GPIO_04; // mit D_in verbinden   MOSI: Master Out Slave In
    private static Pin spiCs = RaspiPin.GPIO_01; // Chip Select

    private final static int DEFAULT_TOLERANCE = 5;   // Tolerance
    private final static long DEFAULT_PAUSE = 500L;

    private Thread parentToNotify = null;

    private AdcChannel[] adcChannel; // Between 0 and 7, 8 channels on the MCP3008

    private static GpioPinDigitalInput misoInput = null;
    private static GpioPinDigitalOutput mosiOutput = null;
    private static GpioPinDigitalOutput clockOutput = null;
    private static GpioPinDigitalOutput chipSelectOutput = null;

    private boolean go = true;

    private List<AdcListener> listeners = new ArrayList<>();

    public AdcObserver() {
        super();
    }

    public AdcObserver(AdcChannel channel) {
        this(new AdcChannel[]{channel});
    }

    public AdcObserver(AdcChannel channel, Pin clk, Pin miso, Pin mosi, Pin cs) {
        this(new AdcChannel[]{channel}, clk, miso, mosi, cs);
    }

    public AdcObserver(AdcChannel[] channel) {
        adcChannel = channel;
    }

    public AdcObserver(AdcChannel[] channel, Pin clk, Pin miso, Pin mosi, Pin cs) {
        adcChannel = channel;
        spiClk = clk;
        spiMiso = miso;
        spiMosi = mosi;
        spiCs = cs;
    }

    public void addListener(AdcListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AdcListener listener) {
        listeners.remove(listener);
    }

    public void fireValueChanged(AdcChannel channel, int newValue) {
        for (AdcListener listener : listeners) {
            listener.valueChanged(channel, newValue);
        }
    }

    public void start() {
        start(DEFAULT_TOLERANCE, DEFAULT_PAUSE);
    }

    public void start(int tol) {
        start(tol, DEFAULT_PAUSE);
    }

    public void start(long p) {
        start(DEFAULT_TOLERANCE, p);
    }

    /**
     * @param tol   Tolerance. Broadcast the fireValueChanged event when the absolute value of the difference between
     *              the last and the current value is greater or equal to this value. This is the value coming from the
     *              ADC, 0..1023. Default is 5
     * @param pause Pause between loops, in ms
     */
    public void start(int tol, long pause) {
        GpioController gpio = GpioFactory.getInstance();
        mosiOutput = gpio.provisionDigitalOutputPin(spiMosi, "MOSI", PinState.LOW);
        clockOutput = gpio.provisionDigitalOutputPin(spiClk, "CLK", PinState.LOW);
        chipSelectOutput = gpio.provisionDigitalOutputPin(spiCs, "CS", PinState.LOW);

        misoInput = gpio.provisionDigitalInputPin(spiMiso, "MISO");

        int lastRead[] = new int[adcChannel.length];
        for (int i = 0; i < lastRead.length; i++)
            lastRead[i] = 0;
        int tolerance = tol;
        while (go) {
            for (int i = 0; i < adcChannel.length; i++) {
                int adc = readAdc(adcChannel[i]);
                //    System.out.println(">>> DEBUG >>> ADC:" + adc);
                int postAdjust = Math.abs(adc - lastRead[i]);
                if (postAdjust > tolerance || tolerance < 0) {
                    fireValueChanged(adcChannel[i], adc);
                    lastRead[i] = adc;
                }
            }
            if (pause > 0L) {
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        System.out.println("Shutting down the GPIO ports...");
        gpio.shutdown();
        if (parentToNotify != null) {
            synchronized (parentToNotify) {
                parentToNotify.notify();
            }
        }
    }

    public void stop() {
        stop(null);
    }

    public void stop(Thread toNotify) {
        go = false;
        parentToNotify = toNotify;
    }

    private int readAdc(AdcChannel channel) {
        chipSelectOutput.high();

        clockOutput.low();
        chipSelectOutput.low();

        int adccommand = channel.channel();
        adccommand |= 0x18; // 0x18: 00011000
        adccommand <<= 3;
        // Send 5 bits: 8 - 3. 8 input channels on the MCP3008.
        for (int i = 0; i < 5; i++) //
        {
            if ((adccommand & 0x80) != 0x0) // 0x80 = 0&10000000
                mosiOutput.high();
            else
                mosiOutput.low();
            adccommand <<= 1;
            clockOutput.high();
            clockOutput.low();
        }

        int adcOut = 0;
        for (int i = 0; i < 12; i++) // Read in one empty bit, one null bit and 10 ADC bits
        {
            clockOutput.high();
            clockOutput.low();
            adcOut <<= 1;

            if (misoInput.isHigh()) {
//      System.out.println("    " + misoInput.getName() + " is high (i:" + i + ")");
                // Shift one bit on the adcOut
                adcOut |= 0x1;
            }
            if (DISPLAY_DIGIT)
                System.out.println("ADCOUT: 0x" + Integer.toString(adcOut, 16).toUpperCase() +
                        ", 0&" + Integer.toString(adcOut, 2).toUpperCase());
        }
        chipSelectOutput.high();

        adcOut >>= 1; // Drop first bit
        return adcOut;
    }
}
