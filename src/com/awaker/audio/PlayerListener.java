package com.awaker.audio;

public interface PlayerListener {
    void newSamples(short[] samples);

    void reportAudioParams(int sampleRate, float msPerFrame);

    default void playbackStarted(int positionMs) {
    }

    default void playbackFinished() {
    }

    default void playbackStopped() {
    }

    default void playbackPaused() {
    }
}
