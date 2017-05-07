package com.awaker.audio_in;

import com.awaker.analyzer.jit.SampleAnalyzeProxy;
import com.awaker.automation.EnvironmentEventListener;
import com.awaker.config.Config;
import com.awaker.config.ConfigChangeListener;
import com.awaker.config.ConfigKey;
import com.awaker.util.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioCapture implements ConfigChangeListener {
    private final SampleAnalyzeProxy analyzer;
    private Thread thread;
    private boolean shouldRun = false;

    public AudioCapture(EnvironmentEventListener listener) {
        analyzer = new SampleAnalyzeProxy(new ClapDetector(listener), 1);
        analyzer.updateAudioParams(44100, 10000000);

        Config.addListener(this, ConfigKey.DETECT_CLAPS);

        if (Config.getBool(ConfigKey.DETECT_CLAPS)) {
            startCapture();
        }
    }

    public static void start(EnvironmentEventListener listener) {
        new AudioCapture(listener);
    }

    private void startCapture() {
        stopCapture();
        shouldRun = true;
        thread = new Thread(this::capture);
        thread.start();
    }

    private void capture() {
        Log.message("Starting audio capture");
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

            while (!thread.isInterrupted() && shouldRun) {
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
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private void stopCapture() {
        shouldRun = false;
        if (thread != null) {
            Log.message("stopping audio capture");
            thread.interrupt();
        }
    }

    @Override
    public void configChanged(ConfigKey key) {
        if (Config.getBool(ConfigKey.DETECT_CLAPS)) {
            if (thread == null || !thread.isAlive()) {
                startCapture();
            }
        } else {
            stopCapture();
        }
    }
}
