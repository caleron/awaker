package com.awaker.audio;

import java.util.*;

public class FFTAnalyzer {

    public static final int SAMPLE_RATE = 44100;

    private final short[] buffer;
    private int bufferedSampleCount = 0;

    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    //Samples für einen Channel, also insgesamt 2048 werden gebraucht
    private static final int MIN_ANALYZE_SIZE = 1024;
    private static final int BUFFER_SIZE = MIN_ANALYZE_SIZE * 2;

    ResultListener listener;

    Map<Integer, FFT> fftMap = new HashMap<>(3);

    public FFTAnalyzer(ResultListener listener) {
        buffer = new short[BUFFER_SIZE];
        this.listener = listener;
    }

    /**
     * Fügt Stereo-Samples ein
     *
     * @param samples der Sample-Array, wobei die Samples abwechselnd für einen Kanal stehen
     */
    public synchronized void pushSamples(short[] samples) {
        final int channels = 2;

        if (bufferedSampleCount + samples.length > MIN_ANALYZE_SIZE * channels) {
            int newSamplesCount;
            short[] analyzeSamples;

            if (bufferedSampleCount + samples.length > MIN_ANALYZE_SIZE * channels * 2) {
                analyzeSamples = new short[1024 * 2 * channels];
                newSamplesCount = MIN_ANALYZE_SIZE * channels * 2 - bufferedSampleCount;
            } else {
                analyzeSamples = new short[1024 * channels];
                newSamplesCount = MIN_ANALYZE_SIZE * channels - bufferedSampleCount;
            }

            //Samples aus dem buffer schreiben
            System.arraycopy(buffer, 0, analyzeSamples, 0, bufferedSampleCount);
            //Samples im Buffer = bufferedSampleCount

            //Neue Samples schreiben
            System.arraycopy(samples, 0, analyzeSamples, bufferedSampleCount, newSamplesCount);
            //Samples im Buffer = bufferedSampleCount + newSamplescount = bufferedSampleCount + 1024*2 - bufferedSampleCount
            //  = 1024*2 (oder 1024 * 2 * 2) 1024 hier als MIN_ANALYZE_SIZE

            //Überschüssige neue Samples in den buffer schreiben
            //Alle Samples aus dem Buffer wurden verwendet, deshalb alte überschreiben
            bufferedSampleCount = samples.length - newSamplesCount;
            System.arraycopy(samples, newSamplesCount, buffer, 0, bufferedSampleCount);

            //Samples analysieren
            analyzeSamples(analyzeSamples);
        } else {
            //Nicht genug Samples für eine Analyse da, neue Samples in Buffer schreiben
            System.arraycopy(samples, 0, buffer, bufferedSampleCount, samples.length);
            bufferedSampleCount += samples.length;
        }
    }

    /**
     * Startet die Analyse von Stereo-Samples
     *
     * @param samples Array aus Samples
     */
    private void analyzeSamples(short[] samples) {
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
            double freq = ((1.0 * SAMPLE_RATE) / (1.0 * sampleFrame)) * i;

            result.add(new AbstractMap.SimpleEntry<>(freq, left[i] + right[i]));
        }

        //List<Map.Entry<Double, Double>> maximaList = findLocalMaxima(result);
        listener.newResults(result);
    }


    /**
     * samples.length = sampleframe
     *
     * @param samples Array aus Samples
     */
    private double[] analyzeChannel(short[] samples) {
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
     * samples.length = sampleframe
     *
     * @param samples Array aus Samples
     */
    public List<Map.Entry<Double, Double>> analyzeChannelOld(short[] samples) {
        int sampleFrame = samples.length;
        double[] amps = analyzeChannel(samples);

        List<Map.Entry<Double, Double>> result = new ArrayList<>();
        for (int i = 0; i < amps.length; i++) {
            //Frequenz entspricht SAMPLE_RATE / sampleFrame * Index
            double freq = ((1.0 * SAMPLE_RATE) / (1.0 * sampleFrame)) * i;

            result.add(new AbstractMap.SimpleEntry<>(freq, amps[i]));
        }

        return findLocalMaxima(result);
    }

    /**
     * Findet Einträge, die größer sind als beide Benachbarten und filtert alle raus, die kleiner als 1% des größten
     * Maximums sind.
     *
     * @param list Die Liste der Frequenz-Amplituden-Paare
     */
    private static List<Map.Entry<Double, Double>> findLocalMaxima(List<Map.Entry<Double, Double>> list) {
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
            if (maximaList.get(i).getValue() < greatest.getValue() * 0.01) {
                maximaList.remove(i);
                i--;
            }
        }
        //System.out.println(maximaList);
        return maximaList;
    }

    public interface ResultListener {
        void newResults(List<Map.Entry<Double, Double>> list);
    }
}
