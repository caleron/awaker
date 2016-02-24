package com.awaker.test;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.analyzer.ResultListener;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestWav implements ResultListener {

    private final int BUFFER_SIZE = 4096;
    private File soundFile;
    private AudioInputStream audioStream;
    private AudioFormat audioFormat;
    private SourceDataLine sourceLine;

    private FFTAnalyzer analyzer;

    public TestWav() {
        analyzer = new FFTAnalyzer(this);

        String strFilename = "media/music.wav";

        try {
            soundFile = new File(strFilename);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            audioStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        audioFormat = audioStream.getFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        sourceLine.start();

        int nBytesRead = 0;
        byte[] abData = new byte[BUFFER_SIZE];
        short[] samples = new short[BUFFER_SIZE / 2];
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) (((abData[i * 2 + 1] & 0xFF) << 8) | (abData[i * 2] & 0xFF));
            }
            analyzer.pushSamples(samples);

            if (nBytesRead >= 0) {
                @SuppressWarnings("unused")
                int nBytesWritten = sourceLine.write(abData, 0, nBytesRead);
            }
        }

        sourceLine.drain();
        sourceLine.close();
    }

    public static void main(String[] args) {
        TestWav wav = new TestWav();
    }


    @Override
    public void newResults(List<Map.Entry<Double, Double>> list) {
        System.out.println(list);
    }
}
