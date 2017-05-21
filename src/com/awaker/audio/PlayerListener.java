package com.awaker.audio;

public interface PlayerListener {
    void newSamples(short[] samples);

    void reportAudioParams(int sampleRate, float msPerFrame);

    void playbackStarted(int positionMs);

    void playbackFinished();

    void playbackStopped();

    void playbackPaused();
}
