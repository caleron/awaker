package com.awaker;

import com.awaker.audio.FFTAnalyzer;

public class SignalGenerator {
    int sampleCount;
    short[] samples;

    public SignalGenerator(int sampleCount) {
        this.sampleCount = sampleCount;
        samples = new short[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            samples[i] = 0;
        }
    }

    public static SignalGenerator create(int sampleCount) {
        return new SignalGenerator(sampleCount);
    }

    public SignalGenerator addFrequency(int freq, int amp) {
        final double wellenlänge = FFTAnalyzer.SAMPLE_RATE / freq;

        for (int i = 0; i < sampleCount; i++) {
            samples[i] += (short) (Math.sin((i / wellenlänge) * Math.PI * 2) * amp);
        }
        return this;
    }

    public short[] getSamples() {
        return samples;
    }
}
