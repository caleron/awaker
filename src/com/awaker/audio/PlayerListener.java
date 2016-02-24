package com.awaker.audio;

public interface PlayerListener {
    void newSamples(short[] samples);

    void playbackStarted();

    void playbackFinished();

    void playbackStopped();

    void playbackPaused();
}
