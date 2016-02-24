package com.awaker.analyzer;

import java.util.*;

class FFTAnalyzeThread extends Thread {
    public static final int SAMPLE_RATE = FFTAnalyzer.SAMPLE_RATE;

    private Queue<short[]> queue;

    private Map<Integer, FFT> fftMap = new HashMap<>(3);

    private ResultListener listener;

    /**
     * Wenn standby, läuft der Thread im Standby, also prüft nicht auf neue Samples.
     */
    private boolean standby = true;

    /**
     * Dadurch wird die Analyse um etwa 12 * 23ms verzögert und damit synchron zur Tonausgabe.
     */
    private static final int ANALYZE_THRESHOLD = 12;

    public FFTAnalyzeThread(ResultListener listener) {
        this.listener = listener;
        queue = new LinkedList<>();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                while (standby) {
                    //Standby, solange keine Samples kommen
                    sleep(5);
                }

                if (queue.size() > ANALYZE_THRESHOLD) {
                    //Samples aus der Queue nehmen
                    short[] samples = queue.poll();

                    //Spielzeit der Samples berechnen und 1 ms abziehen
                    long samplePlayTime = (long) (((samples.length * 0.5 * 1000) / (SAMPLE_RATE * 1.0)) - 1);

                    if (queue.size() > ANALYZE_THRESHOLD + 10) {
                        //Falls zu viele Samples drin sind, Wartezeit reduzieren
                        samplePlayTime = Math.max(samplePlayTime - 10, 0);
                        System.out.println("shit: " + queue.size());
                    }

                    long start = System.nanoTime();
                    //Samples analysieren
                    analyzeSamples(samples);

                    //Berechnungszeit in ms bestimmen
                    int calculationDuration = (int) ((System.nanoTime() - start) / 1000000.0);

                    //Schlafen
                    sleep(samplePlayTime - calculationDuration);

                } else {
                    //Schlafen, wenn nicht genug Samples da sind
                    sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void pushAnalyzeArray(short[] samples) {
        standby = false;
        queue.add(samples);
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
}
