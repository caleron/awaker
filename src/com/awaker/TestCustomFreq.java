package com.awaker;

import com.awaker.princeton.Complex;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

@SuppressWarnings("Duplicates")
public class TestCustomFreq {

    static final int sampleRate = 44100;
    static final int sampleFrame = 2048;

    //sinus verwendet Bogenmaß, also mit Pi bzw. Wellenlänge = 2*PI
    public static void main(String[] args) {

        short[] samples = new short[sampleFrame];

        for (int i = 0; i < sampleFrame; i++) {
            samples[i] = 0;
        }

        addFrequency(samples, 300, 8000);
        addFrequency(samples, 200, 5000);
        addFrequency(samples, 50, 1000);

        analyze2(samples);

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 1, 2, sampleRate, false);
        SourceDataLine sourceLine;

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
            sourceLine.start();

            int count = 0;
            while (count < 1000) {
                sourceLine.write(toByteArray(samples), 0, sampleFrame);
                count++;
            }

            sourceLine.drain();
            sourceLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void addFrequency(short[] samples, int freq, int amp) {
        final double wellenlänge = sampleRate / freq;

        for (int i = 0; i < sampleFrame; i++) {
            samples[i] += (short) (Math.sin((i / wellenlänge) * Math.PI * 2) * amp);
        }
    }

    static void analyze(short[] samples) {
        FFT fft = new FFT(samples.length);

        double[] real = new double[samples.length];
        double[] imag = new double[samples.length];

        for (int i = 0; i < samples.length; i++) {
            real[i] = samples[i];
            imag[i] = 0;
        }

        fft.fft(real, imag);

        Map<Integer, Double> map = new HashMap<>(samples.length);
        for (int i = 0; i < samples.length / 2; i++) {
            map.put(i, Math.log10(Math.abs(real[i])));
        }

        List<Map.Entry<Integer, Double>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        printFreqAmpPair(list.get(0));
        printFreqAmpPair(list.get(1));
        printFreqAmpPair(list.get(2));
        printFreqAmpPair(list.get(3));
        printFreqAmpPair(list.get(4));
        printFreqAmpPair(list.get(5));
        printFreqAmpPair(list.get(6));
    }


    static void analyze2(short[] samples) {
        FFT fft = new FFT(samples.length);

        double[] real = new double[samples.length];
        double[] imag = new double[samples.length];

        for (int i = 0; i < samples.length; i++) {
            real[i] = samples[i];
            imag[i] = 0;
        }

        fft.fft(real, imag);

        Map<Integer, Double> map = new HashMap<>(samples.length);
        for (int i = 0; i < samples.length / 2; i++) {
            map.put(i, Math.hypot(real[i], imag[i]));
        }

        List<Map.Entry<Integer, Double>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        printFreqAmpPair(list.get(0));
        printFreqAmpPair(list.get(1));
        printFreqAmpPair(list.get(2));
        printFreqAmpPair(list.get(3));
        printFreqAmpPair(list.get(4));
        printFreqAmpPair(list.get(5));
        printFreqAmpPair(list.get(6));
    }

    static void analyzePrinceton(short[] samples) {
        Complex[] comp = new Complex[samples.length];

        for (int i = 0; i < samples.length; i++) {
            comp[i] = new Complex(samples[i], 0);
        }

        Complex[] out = com.awaker.princeton.FFT.fft(comp);

        Map<Integer, Double> map = new HashMap<>(samples.length);
        for (int i = 0; i < samples.length / 2; i++) {
            map.put(i, out[i].abs());
        }

        List<Map.Entry<Integer, Double>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        printFreqAmpPair(list.get(0));
        printFreqAmpPair(list.get(1));
        printFreqAmpPair(list.get(2));
        printFreqAmpPair(list.get(3));
        printFreqAmpPair(list.get(4));
        printFreqAmpPair(list.get(5));
        printFreqAmpPair(list.get(6));
    }

    static byte[] toByteArray(short[] samples) {
        byte[] b = new byte[samples.length * 2 + 1];
        short s;
        for (int i = 0; i < samples.length; ) {
            s = samples[i];
            b[i++] = (byte) s;
            b[i++] = (byte) (s >>> 8);
        }
        return b;
    }

    static void printFreqAmpPair(Map.Entry<Integer, Double> entry) {
        double freq = ((1.0 * sampleRate) / (1.0 * sampleFrame)) * entry.getKey();

        System.out.println(freq + " = " + entry.getValue());
    }
}
