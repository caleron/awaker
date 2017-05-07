package com.awaker.analyzer.jit;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.SampleQuantizer;

/**
 * Class that controls the {@link SampleAnalyzeThread} and feeds it with new quantized samples.
 */
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

        //start the new thread
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

    /**
     * Should be called each time a new track is played.
     *
     * @param sampleRate the sample rate
     * @param msPerFrame the number of ms per frame
     */
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
