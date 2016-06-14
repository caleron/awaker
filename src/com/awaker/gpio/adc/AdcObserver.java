package com.awaker.gpio.adc;

import com.pi4j.io.gpio.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Read an Analog to Digital Converter
 * <p>
 * http://www.lediouris.net/RaspberryPI/index.html https://github.com/OlivierLD/raspberry-pi4j-samples
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

    private final static int DEFAULT_TOLERANCE = 5;
    private final static long DEFAULT_PAUSE = 500L;

    private int tolerance = DEFAULT_TOLERANCE;
    private long pause = DEFAULT_PAUSE;

    private final Thread readingThread;

    private AdcChannel[] adcChannel; // Between 0 and 7, 8 channels on the MCP3008

    private static GpioPinDigitalInput misoInput = null;
    private static GpioPinDigitalOutput mosiOutput = null;
    private static GpioPinDigitalOutput clockOutput = null;
    private static GpioPinDigitalOutput chipSelectOutput = null;

    private boolean go = true;

    private List<AdcListener> listeners = new ArrayList<>();

    /**
     * Erstellt einen neuen {@link AdcObserver}.
     *
     * @param channels Die zu überwachenden Kanäle des ADC
     */
    public AdcObserver(AdcChannel[] channels) {
        adcChannel = channels;
        readingThread = new Thread(this::runMeasuring);
    }

    /**
     * Fügt einen neuen {@link AdcListener} hinzu.
     *
     * @param listener Der neue {@link AdcListener}
     */
    public void addListener(AdcListener listener) {
        listeners.add(listener);
    }

    /**
     * Benachrichtigt alle {@link AdcListener} über eine Veränderung auf einem Kanal.
     *
     * @param channel  Der Kanal
     * @param newValue Der neue Wert.
     */
    private void fireValueChanged(AdcChannel channel, int newValue) {
        for (AdcListener listener : listeners) {
            listener.valueChanged(channel, newValue);
        }
    }

    /**
     * Setzt Toleranz und Pause.
     *
     * @param tolerance Tolerance. Broadcast the fireValueChanged event when the absolute value of the difference
     *                  between the last and the current value is greater or equal to this value. This is the value
     *                  coming from the ADC, 0..1023. Default is 5
     * @param pause     Pause between loops, in ms
     */
    public void setCustomParams(int tolerance, long pause) {
        this.tolerance = tolerance;
        this.pause = pause;
    }

    /**
     * Startet das Auslesen.
     */
    public void start() {
        readingThread.start();
    }

    /**
     * Liest wiederholt alle eingestellten Kanäle des ADC aus und feuert Events, wenn sich der Wert eines Kanals
     * ändert.
     */
    private void runMeasuring() {
        GpioController gpio = GpioFactory.getInstance();
        mosiOutput = gpio.provisionDigitalOutputPin(spiMosi, "MOSI", PinState.LOW);
        clockOutput = gpio.provisionDigitalOutputPin(spiClk, "CLK", PinState.LOW);
        chipSelectOutput = gpio.provisionDigitalOutputPin(spiCs, "CS", PinState.LOW);

        misoInput = gpio.provisionDigitalInputPin(spiMiso, "MISO");

        int lastRead[] = new int[adcChannel.length];
        for (int i = 0; i < lastRead.length; i++)
            lastRead[i] = 0;

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
        synchronized (readingThread) {
            readingThread.notifyAll();
        }
    }

    /**
     * Stoppt synchron den Lesethread. Wartet maximal 2 * pause.
     */
    public void stop() {
        go = false;
        try {
            readingThread.wait(2 * pause);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Führt einen Lesevorgang auf einem Kanal durch.
     *
     * @param channel Der Kanal
     * @return Der ausgelesene Wert
     */
    private static int readAdc(AdcChannel channel) {
        chipSelectOutput.high();

        clockOutput.low();
        chipSelectOutput.low();

        int adccommand = channel.getNumber();
        adccommand |= 0x18; // 0x18: 00011000
        adccommand <<= 3;

        // Send 5 bits: 8 - 3. 8 input channels on the MCP3008.
        for (int i = 0; i < 5; i++) //
        {
            // 0x80 = 0&10000000
            if ((adccommand & 0x80) != 0x0) {
                mosiOutput.high();
            } else {
                mosiOutput.low();
            }
            adccommand <<= 1;
            clockOutput.high();
            clockOutput.low();
        }

        int adcOut = 0;

        // Read in one empty bit, one null bit and 10 ADC bits
        for (int i = 0; i < 12; i++) {
            clockOutput.high();
            clockOutput.low();
            adcOut <<= 1;

            if (misoInput.isHigh()) {
                //System.out.println("    " + misoInput.getName() + " is high (i:" + i + ")");
                //Shift one bit on the adcOut
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
