package com.awaker.audio;

public interface PlayerListener {
    default void newSamples(short[] samples) {

    }

    default void reportAudioParams(int sampleRate, float msPerFrame) {

    }

    default void playbackStarted(int positionMs) {
    }

    default void playbackFinished() {
    }

    default void playbackStopped() {
    }

    default void playbackPaused() {
    }
}
