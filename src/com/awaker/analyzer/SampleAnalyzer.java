package com.awaker.analyzer;

import java.util.*;

public class SampleAnalyzer {
    private static final Map<Integer, FFT> fftMap = new HashMap<>(3);


    /**
     * Startet die Analyse von Stereo-Samples. Die Amplituden der beiden Kanäle werden einfach addiert.
     * zum Berechnen von dB: 10 * log10(re * re + im * im)
     *
     * @param samples Array aus Samples
     */
    public static List<Map.Entry<Double, Double>> analyzeSamples(short[] samples, int channels, int sampleRate) {
        if (channels == 2) {
            int sampleFrame = samples.length / 2;
            //System.out.println(sampleFrame);
            short[] leftSamples = new short[sampleFrame];
            short[] rightSamples = new short[sampleFrame];

            for (int i = 0; i < leftSamples.length; i++) {
                leftSamples[i] = samples[i * 2];
                rightSamples[i] = samples[i * 2 + 1];
            }

            double[] left = analyzeChannel(leftSamples);
            double[] right = analyzeChannel(rightSamples);

            List<Map.Entry<Double, Double>> result = new ArrayList<>();
            for (int i = 0; i < left.length; i++) {
                //Frequenz entspricht SAMPLE_RATE / sampleFrame * Index
                double freq = ((1.0 * sampleRate) / (1.0 * sampleFrame)) * i;

                result.add(new AbstractMap.SimpleEntry<>(freq, left[i] + right[i]));
            }

            //List<Map.Entry<Double, Double>> maximaList = findLocalMaxima(result);
            return result;
        } else if (channels == 1) {
            double[] mono = analyzeChannel(samples);

            List<Map.Entry<Double, Double>> result = new ArrayList<>();
            for (int i = 0; i < mono.length; i++) {
                //Frequenz entspricht SAMPLE_RATE / sampleFrame * Index
                double freq = ((1.0 * sampleRate) / (1.0 * samples.length)) * i;

                result.add(new AbstractMap.SimpleEntry<>(freq, mono[i]));
            }

            return result;
        }
        return null;
    }


    /**
     * samples.length = sampleframe
     *
     * @param samples Array aus Samples
     */
    private static double[] analyzeChannel(short[] samples) {
        int sampleFrame = samples.length;


        double[] real = new double[sampleFrame];
        double[] imag = new double[sampleFrame];

        for (int i = 0; i < sampleFrame; i++) {
            real[i] = samples[i];
            imag[i] = 0;
        }

        FFT fft;
        if (fftMap.containsKey(sampleFrame)) {
            fft = fftMap.get(sampleFrame);
        } else {
            fft = new FFT(sampleFrame);
            fftMap.put(sampleFrame, fft);
        }

        fft.fft(real, imag);

        double[] amps = new double[sampleFrame / 2];

        //nur die erste hälfte ist wichtig, der Rest ist "gespiegelt"
        for (int i = 0; i < sampleFrame / 2; i++) {
            double amp = Math.hypot(real[i], imag[i]) / sampleFrame;

            amps[i] = amp;
        }
        return amps;
    }


    /**
     * Findet Einträge, die größer sind als beide Benachbarten und filtert alle raus, die kleiner als 1% des größten
     * Maximums sind.
     *
     * @param list Die Liste der Frequenz-Amplituden-Paare
     */
    static List<Map.Entry<Double, Double>> findLocalMaxima(List<Map.Entry<Double, Double>> list, double threshold) {
        List<Map.Entry<Double, Double>> maximaList = new ArrayList<>();

        //lokale Maxima bestimmen
        for (int i = 0; i < list.size(); i++) {

            Map.Entry<Double, Double> prevEntry;
            if (i > 0) {
                prevEntry = list.get(i - 1);
            } else {
                prevEntry = new AbstractMap.SimpleEntry<>(0.0, 0.0);
            }
            Map.Entry<Double, Double> entry = list.get(i);
            Map.Entry<Double, Double> nextEntry;
            if (i + 1 < list.size()) {
                nextEntry = list.get(i + 1);
            } else {
                nextEntry = new AbstractMap.SimpleEntry<>(0.0, 0.0);
            }
            //wenn Wert größer als die Werte links und rechts
            if (entry.getValue() > prevEntry.getValue() && entry.getValue() > nextEntry.getValue()) {
                maximaList.add(entry);
            }
        }

        //das Größte Maxima bestimmen
        Map.Entry<Double, Double> greatest = new AbstractMap.SimpleEntry<>(0.0, 0.0);
        for (Map.Entry<Double, Double> entry : maximaList) {
            if (entry.getValue() > greatest.getValue()) {
                greatest = entry;
            }
        }

        //Alle Maxima entfernen, die kleiner als 1% des größten Maximums sind
        for (int i = 0; i < maximaList.size(); i++) {
            if (maximaList.get(i).getValue() < greatest.getValue() * threshold) {
                maximaList.remove(i);
                i--;
            }
        }
        //System.out.println(maximaList);
        return maximaList;
    }


    /**
     * Findet Einträge, die größer sind als beide Benachbarten und filtert alle raus, die kleiner als 1% des größten
     * Maximums sind.
     *
     * @param list Die Liste der Frequenz-Amplituden-Paare
     */
    private static List<Map.Entry<Double, Double>> findLocalMaxima(List<Map.Entry<Double, Double>> list) {
        return findLocalMaxima(list, 0.01);
    }
}
