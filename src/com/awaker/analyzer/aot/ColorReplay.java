package com.awaker.analyzer.aot;

import com.awaker.analyzer.MusicColorChangeListener;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ColorReplay {

    private final MusicColorChangeListener listener;
    private ByteBuffer buffer;
    private int sleepTimeMicroSeconds;

    private final ScheduledExecutorService replayThread = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;
    private int sampleRate;

    public ColorReplay(MusicColorChangeListener listener) {
        this.listener = listener;
    }

    public void newTrack(byte[] musicColors) {
        if (musicColors == null) {
            throw new RuntimeException("musicColors is null");
        }
        buffer = ByteBuffer.wrap(musicColors);

        //take the first int, which is the sample rate
        sampleRate = buffer.getInt();
        //calculate the sleep time in microseconds
        sleepTimeMicroSeconds = (int) ((1024.0 / sampleRate) * 1000000);
    }

    /**
     * @param positionMs the position in milliseconds
     */
    public void playFromPosition(int positionMs) {
        scheduledFuture.cancel(false);

        //sampleRate / 1024 = colors per second = ints per second
        //colors per second * position in seconds = position in ints
        //position in ints * 4 = position in bytes
        int bufferPosition = (int) ((sampleRate / 1024.0) * (positionMs / 1000.0)) * 4;
        //add 1 for sampleRate int
        bufferPosition++;

        //set the position
        buffer.position(bufferPosition);

        scheduledFuture = replayThread.scheduleAtFixedRate(this::outputNextColor, 0, sleepTimeMicroSeconds, TimeUnit.MICROSECONDS);
    }

    public void play() {
        scheduledFuture.cancel(false);
        scheduledFuture = replayThread.scheduleAtFixedRate(this::outputNextColor, 0, sleepTimeMicroSeconds, TimeUnit.MICROSECONDS);
    }

    public void stop() {
        scheduledFuture.cancel(false);
    }

    private void outputNextColor() {
        Color color = new Color(buffer.getInt());
        listener.newColor(color);
    }
}
