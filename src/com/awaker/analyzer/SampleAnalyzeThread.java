package com.awaker.analyzer;

import com.awaker.Awaker;
import com.awaker.util.Log;

import java.util.*;

class SampleAnalyzeThread extends Thread {
    private final Queue<short[]> queue;
    private final Map<Integer, FFT> fftMap = new HashMap<>(3);
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
                    analyzeSamples(samples);
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
     * ähnliches verwendet, also der Sound erst nach einer Verzögerung abgespielt wird, damit eine reibungslose
     * Wiedergabe garantiert ist. Unter Windows ist diese Verzögerung anders als auf dem Raspberry.
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

    void pushAnalyzeArray(short[] samples) {
        standby = false;
        queue.add(samples);
    }

    /**
     * Startet die Analyse von Stereo-Samples. Die Amplituden der beiden Kanäle werden einfach addiert.
     * zum Berechnen von dB: 10 * log10(re * re + im * im)
     *
     * @param samples Array aus Samples
     */
    private void analyzeSamples(short[] samples) {
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
                double freq = ((1.0 * currentSampleRate) / (1.0 * sampleFrame)) * i;

                result.add(new AbstractMap.SimpleEntry<>(freq, left[i] + right[i]));
            }

            //List<Map.Entry<Double, Double>> maximaList = findLocalMaxima(result);
            listener.newResults(result);
        } else if (channels == 1) {
            double[] mono = analyzeChannel(samples);

            List<Map.Entry<Double, Double>> result = new ArrayList<>();
            for (int i = 0; i < mono.length; i++) {
                //Frequenz entspricht SAMPLE_RATE / sampleFrame * Index
                double freq = ((1.0 * currentSampleRate) / (1.0 * samples.length)) * i;

                result.add(new AbstractMap.SimpleEntry<>(freq, mono[i]));
            }

            listener.newResults(result);
        }
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
