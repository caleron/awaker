package com.awaker.analyzer;

import com.awaker.Awaker;
import com.awaker.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Thread that runs the actual analysis.
 */
class SampleAnalyzeThread extends Thread {
    private final Queue<short[]> queue;
    private final AnalyzeResultListener listener;
    private final int channels;

    private int currentSampleRate = 0;
    private long analyzedSamplesCount = 0;

    /**
     * Wenn standby, läuft der Thread im Standby, also prüft nicht auf neue Samples.
     */
    private boolean standby = true;

    /**
     * Dadurch wird die Analyse um etwa 12 * 23ms verzögert und damit möglichst synchron zur Tonausgabe. Wird durch
     * Aktualisierung von Soundparametern in updateAudioParams angepasst.
     */
    private int analyzeThreshold = 12;

    /**
     * Creates a new {@link SampleAnalyzeThread}.
     *
     * @param listener The listener for the results.
     * @param channels The channel count
     */
    SampleAnalyzeThread(AnalyzeResultListener listener, int channels) {
        this.listener = listener;
        queue = new LinkedList<>();
        this.channels = channels;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                while (standby) {
                    //Standby, solange keine Samples kommen
                    sleep(5);
                }

                if (queue.size() > analyzeThreshold) {
                    //Samples aus der Queue nehmen
                    short[] samples = queue.poll();

                    if (samples == null) {
                        Log.error("got zeros");
                        queue.clear();
                        continue;
                    }

                    //Spielzeit der Samples berechnen und 1 ms abziehen
                    long samplePlayTime = (long) (((samples.length * 0.5 * 1000) / (currentSampleRate * 1.0)) - 1);

                    if (queue.size() > analyzeThreshold + 10) {
                        //Falls zu viele Samples drin sind, Wartezeit reduzieren
                        samplePlayTime = Math.max(samplePlayTime - 20, 0);
                    }

                    long start = System.nanoTime();
                    //Samples analysieren
                    listener.newResults(SampleAnalyzer.analyzeSamples(samples, channels, currentSampleRate));
                    analyzedSamplesCount += samples.length / 2;

                    //Berechnungszeit in ms bestimmen
                    int calculationDuration = (int) ((System.nanoTime() - start) / 1000000.0);

                    //Schlafen
                    long sleepTime = samplePlayTime - calculationDuration;
                    if (sleepTime > 0) {
                        sleep(sleepTime);
                    }
                } else {
                    //Schlafen, wenn nicht genug Samples da sind
                    sleep(1);
                }
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    /**
     * Wird aufgerufen, um aktuelle Audioparameter zu übermitteln. Mit diesen Informationen wird die Analyse mit der
     * Musik synchronisiert. Dies ist notwendig, da der Audiomixer (etwa Alsamixer auf dem Raspberry) einen Puffer oder
     * ähnliches verwendet, also der Sound erst nach einer Verzögerung abgespielt wird (damit wohl eine reibungslose
     * Wiedergabe garantiert ist). Unter Windows ist diese Verzögerung anders als auf dem Raspberry.
     *
     * @param sampleRate Die aktuelle Samplerate
     * @param msPerFrame Zeit pro Frame (und damit Sampleblock) in ms
     */
    void updateAudioParams(int sampleRate, float msPerFrame) {
        if (currentSampleRate != sampleRate && sampleRate > 0) {
            currentSampleRate = sampleRate;

            //verzögerung in ms
            final int delay;
            if (Awaker.isMSWindows) {
                delay = 440;
            } else {
                delay = 350;
            }

            analyzeThreshold = (int) (delay / msPerFrame);
            Log.message("analyzeThreshold = " + analyzeThreshold);
        }
    }

    /**
     * Adds a new sample array for frequency analysis.
     *
     * @param samples array of samples which size is a power of 2.
     */
    void pushAnalyzeArray(short[] samples) {
        standby = false;
        queue.add(samples);
    }


    /**
     * returns the analyzed sample count.
     *
     * @return number of analyzed samples since the last reset.
     */
    long getAnalyzedSamplesCount() {
        return analyzedSamplesCount;
    }

    /**
     * Setzt den Samplezähler und die Warteschlange zurück
     */
    void reset() {
        analyzedSamplesCount = 0;
        queue.clear();
    }
}
