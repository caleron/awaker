package com.awaker.analyzer;

import javazoom.jl.decoder.*;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PreAnalyzer {

    /**
     * The MPEG audio decoder.
     */
    private final Decoder decoder;
    private int sampleRate;

    private final SampleQuantizer quantizer;
    private List<Byte> outputArray;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public PreAnalyzer() {
        decoder = new Decoder();
        quantizer = new SampleQuantizer(2);
    }

    /**
     * Decodes a single frame.
     */
    public List<Byte> analyze(InputStream stream) {
        long start = System.currentTimeMillis();
        try {
            Bitstream bitstream = new Bitstream(stream);

            outputArray = new ArrayList<>(10000);

            //read first frame
            Header header = bitstream.readFrame();
            sampleRate = header.frequency();
            //put in samplerate
            addInt(sampleRate);

            while (header != null) {
                //decode next frame
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                short[] samples = output.getBuffer();

                collectSamples(samples);

                //close the frame
                bitstream.closeFrame();

                //read next frame
                header = bitstream.readFrame();
            }
            long diff = System.currentTimeMillis() - start;
            return outputArray;
        } catch (JavaLayerException ex) {
            return null;
        }
    }


    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    //Samples für einen Channel, also insgesamt 2048 werden gebraucht

    private void collectSamples(short[] samples) {
        short[] quantizedSamples = quantizer.quantize(samples);
        if (quantizedSamples.length == 2048) {
            analyzeSamples(quantizedSamples);

        } else if (quantizedSamples.length == 4096) {
            analyzeSamples(Arrays.copyOfRange(quantizedSamples, 0, 2048));
            analyzeSamples(Arrays.copyOfRange(quantizedSamples, 2048, 4096));

        } else if (quantizedSamples.length > 4096) {
            throw new RuntimeException("unknown sample count" + quantizedSamples.length);
        }
    }

    private void analyzeSamples(short[] samples) {
        //analyze samples
        List<Map.Entry<Double, Double>> result = SampleAnalyzer.analyzeSamples(samples, 2, sampleRate);
        //translate to color
        Color color = ColorTranslator.translatePartition2(result);
        //write color
        addInt(color.getRGB());
    }

    private void addInt(int value) {
        outputArray.add((byte) (value >>> 24));
        outputArray.add((byte) (value >>> 16));
        outputArray.add((byte) (value >>> 8));
        outputArray.add((byte) value);
    }
}
