package com.awaker.test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

@SuppressWarnings("Duplicates")
public class TestCustomFreq {

    static final int sampleRate = 44100;
    static final int sampleFrame = 2048 * 128;

    //sinus verwendet Bogenmaß, also mit Pi bzw. Wellenlänge = 2*PI

    /**
     * Klingt richtig, aber die Analyse liefert falsches Ergebnis Beim SignalGenerator wurde der Faktor 2 (bei Pi) in
     * der Berechnung entfernt. Dadurch klingt der Ton richtig, aber die Analyse durch FFT stellt ein Maximum bei der
     * Frequenz/2 fest. Die Frequenz einer festen mp3-Datei mit 440Hz wird aber richtig erkannt. Möglicherweise liegt
     * der Fehler in der Weitergabe der Samples an das Audiogerät, denn der Faktor 2 bei SignalGenerator sollte richtig
     * sein.
     *
     * @param args
     */
    public static void main(String[] args) {

        short[] samples = SignalGenerator.create(sampleFrame).addFrequency(440, 3000).getStereoSamples();

        //FFTAnalyzer fftAnalyzer = new FFTAnalyzer(System.out::println);
        //fftAnalyzer.pushSamples(samples);

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 2, 4, sampleRate, false);
        SourceDataLine sourceLine;

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
            sourceLine.start();

            int count = 10;
            while (count < 1000) {
                sourceLine.write(toByteArray(samples), 0, sampleFrame * 2);
                count++;
            }

            sourceLine.drain();
            sourceLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
