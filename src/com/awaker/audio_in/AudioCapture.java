package com.awaker.audio_in;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.automation.EnvironmentEventListener;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioCapture {

    private boolean interrupt = false;

    private final FFTAnalyzer analyzer;

    public AudioCapture(EnvironmentEventListener listener) {
        analyzer = new FFTAnalyzer(new ClapDetector(listener), 1);
        analyzer.updateAudioParams(44100, 10000000);
    }

    public void start() {
        interrupt = false;
        new Thread(this::capture).start();
    }

    private void capture() {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        TargetDataLine line;
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);

            line.start();
            //für eine Sekunde: int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
            int bufferSize = 2048;
            byte byteBuffer[] = new byte[bufferSize];

            while (!interrupt) {
                int count = line.read(byteBuffer, 0, byteBuffer.length);

                if (count > 0) {
                    ShortBuffer shortBuff = ByteBuffer.wrap(byteBuffer, 0, count).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    short shortBuffer[] = new short[bufferSize / 2];

                    for (int i = 0; shortBuff.hasRemaining(); i++) {
                        shortBuffer[i] = shortBuff.get();
                    }
                    analyzer.pushSamples(shortBuffer);
                }
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        interrupt = true;
    }

}
