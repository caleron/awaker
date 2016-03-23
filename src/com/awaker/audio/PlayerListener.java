package com.awaker.audio;

interface PlayerListener {
    void newSamples(short[] samples);

    void reportAudioParams(int sampleRate, float msPerFrame);

    void playbackStarted();

    void playbackFinished();

    void playbackStopped();

    void playbackPaused();
}
