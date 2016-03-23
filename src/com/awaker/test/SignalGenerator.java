package com.awaker.test;

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
        final double wellenlänge = 44100 / freq;

        for (int i = 0; i < sampleCount; i++) {
            samples[i] += (short) (Math.sin((i / wellenlänge) * Math.PI) * amp);
        }
        return this;
    }

    public short[] getStereoSamples() {
        short[] stereoSamples = new short[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            stereoSamples[i * 2] = samples[i];
            stereoSamples[i * 2 + 1] = samples[i];
        }
        return stereoSamples;
    }

    public short[] getMonoSamples() {
        return samples;
    }
}
