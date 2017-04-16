package com.awaker.analyzer;

public class SampleQuantizer {

    private final short[] buffer;
    private int bufferedSampleCount = 0;

    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    //Samples für einen Channel, also insgesamt 2048 werden gebraucht
    private static final int MIN_ANALYZE_SIZE = 1024;

    private final int channels;
    private final SampleAnalyzeThread analyzeThread;

    public SampleQuantizer(AnalyzeResultListener listener) {
        this(listener, 2);
    }

    public SampleQuantizer(AnalyzeResultListener listener, int channels) {
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Invalid Channel count, only mono and stereo is supported");
        }
        this.channels = channels;
        int bufferSize = MIN_ANALYZE_SIZE * channels;

        buffer = new short[bufferSize];
        analyzeThread = new SampleAnalyzeThread(listener, channels);
        analyzeThread.start();
    }

    /**
     * Fügt Stereo-Samples ein
     *
     * @param samples der Sample-Array, wobei die Samples abwechselnd für einen Kanal stehen
     */
    public synchronized void pushSamples(short[] samples) {

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

    public void updateAudioParams(int sampleRate, float msPerFrame) {
        analyzeThread.updateAudioParams(sampleRate, msPerFrame);
    }

    public long getAnalyzedSamplesCount() {
        return analyzeThread.getAnalyzedSamplesCount();
    }

    /**
     * Setzt den Samplezähler und die Warteschlange zurück
     */
    public void reset() {
        analyzeThread.reset();
    }

}
