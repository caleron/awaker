package com.awaker.analyzer;

public class SampleAnalyzeProxy {

    private final SampleAnalyzeThread analyzeThread;
    private final SampleQuantizer quantizer;

    public SampleAnalyzeProxy(AnalyzeResultListener listener) {
        this(listener, 2);
    }

    public SampleAnalyzeProxy(AnalyzeResultListener listener, int channels) {
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Invalid Channel count, only mono and stereo is supported");
        }
        quantizer = new SampleQuantizer(channels);

        analyzeThread = new SampleAnalyzeThread(listener, channels);
        analyzeThread.start();
    }

    /**
     * Fügt Stereo-Samples ein
     *
     * @param samples der Sample-Array, wobei die Samples abwechselnd für einen Kanal stehen
     */
    public synchronized void pushSamples(short[] samples) {
        short[] quantizedSamples = quantizer.quantize(samples);
        if (quantizedSamples != null) {
            analyzeThread.pushAnalyzeArray(quantizedSamples);
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
