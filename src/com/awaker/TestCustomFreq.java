package com.awaker;

import com.awaker.analyzer.FFTAnalyzer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

@SuppressWarnings("Duplicates")
public class TestCustomFreq {

    static final int sampleRate = 44100;
    static final int sampleFrame = 44100 * 5;

    //sinus verwendet Bogenmaß, also mit Pi bzw. Wellenlänge = 2*PI
    public static void main(String[] args) {

        short[] samples = SignalGenerator.create(sampleFrame).addFrequency(400, 5000)
                .addFrequency(4000, 500).addFrequency(100, 5000).getSamples();

        //FFTAnalyzer fftAnalyzer = new FFTAnalyzer(null);

        //fftAnalyzer.analyzeChannelOld(samples);

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 1, 2, sampleRate, false);
        SourceDataLine sourceLine;

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
            sourceLine.start();

            int count = 0;
            while (count < 1000) {
                sourceLine.write(toByteArray(samples), 0, sampleFrame);
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
