package com.awaker.test;

import com.awaker.analyzer.FFT;
import com.awaker.princeton.Complex;

public class FFTBenchmark {
    static final int sampleRate = 44100;
    static final int sampleFrame = 1024;
    static final int freq = 3000;
    static final int amp = 2000;

    //Columbia braucht ~200ms, Princeton über 8 Sekunden
    public static void main(String[] args) {
        short[] samples = SignalGenerator.create(sampleFrame).addFrequency(3000, 2000)
                .addFrequency(500, 5000).addFrequency(600, 3000).getMonoSamples();

        FFT fft = new FFT(sampleFrame);

        double[] real = new double[sampleFrame];
        double[] imag = new double[sampleFrame];

        Complex[] complices = new Complex[sampleFrame];

        for (int i = 0; i < sampleFrame; i++) {
            real[i] = samples[i];
            imag[i] = 0;
            complices[i] = new Complex(samples[i], 0);
        }

        final int TEST_COUNT = 10000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < TEST_COUNT; i++) {
            fft.fft(real, imag);
        }
        System.out.println("Columbia FFT " + TEST_COUNT + " times: " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < TEST_COUNT; i++) {
            com.awaker.princeton.FFT.fft(complices);
        }
        System.out.println("Princeton FFT " + TEST_COUNT + " times: " + (System.currentTimeMillis() - start) + "ms");


    }

}
