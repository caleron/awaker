package com.awaker.analyzer;

import java.util.List;
import java.util.Map;

public class FFTAnalyzer {

    public static final int SAMPLE_RATE = 44100;

    private final short[] buffer;
    private int bufferedSampleCount = 0;

    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    //Samples für einen Channel, also insgesamt 2048 werden gebraucht
    private static final int MIN_ANALYZE_SIZE = 1024;
    private static final int BUFFER_SIZE = MIN_ANALYZE_SIZE * 2;

    FFTAnalyzeThread analyzeThread;

    public FFTAnalyzer(ResultListener listener) {
        buffer = new short[BUFFER_SIZE];
        analyzeThread = new FFTAnalyzeThread(listener);
        analyzeThread.start();
    }

    /**
     * Fügt Stereo-Samples ein
     *
     * @param samples der Sample-Array, wobei die Samples abwechselnd für einen Kanal stehen
     */
    public synchronized void pushSamples(short[] samples) {
        final int channels = 2;

        if (bufferedSampleCount + samples.length >= MIN_ANALYZE_SIZE * channels) {
            int newSamplesCount;
            short[] analyzeSamples;

            if (bufferedSampleCount + samples.length >= MIN_ANALYZE_SIZE * channels * 2) {
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

            analyzeThread.pushAnalyzeArray(analyzeSamples);
        } else {
            //Nicht genug Samples für eine Analyse da, neue Samples in Buffer schreiben
            System.arraycopy(samples, 0, buffer, bufferedSampleCount, samples.length);
            bufferedSampleCount += samples.length;
        }
    }

    public List<Map.Entry<Double, Double>> analyzeChannelOld(short[] samples) {
        return analyzeThread.analyzeChannelOld(samples);
    }
}
