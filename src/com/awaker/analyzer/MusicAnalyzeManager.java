package com.awaker.analyzer;

import com.awaker.analyzer.aot.ColorReplay;
import com.awaker.analyzer.jit.SampleAnalyzeProxy;
import com.awaker.audio.PlayerListener;
import com.awaker.data.DbManager;
import com.awaker.data.TrackWrapper;
import com.awaker.util.Log;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Abstraction layer to switch between jit-analyzed colors and aot-analyzed colors
 */
public class MusicAnalyzeManager implements PlayerListener, AnalyzeResultListener {
    private final MusicColorChangeListener listener;

    private final SampleAnalyzeProxy analyzer;
    private final ColorReplay colorReplay;

    private boolean useAotData = false;
    private boolean isEnabled = true;

    public MusicAnalyzeManager(MusicColorChangeListener colorChangeListener) {
        this.listener = colorChangeListener;
        analyzer = new SampleAnalyzeProxy(this);
        colorReplay = new ColorReplay(colorChangeListener);
    }

    public void setEnabled(boolean enabled, int positionMs) {
        this.isEnabled = enabled;
        if (useAotData) {
            if (enabled && positionMs >= 0) {
                colorReplay.playFromPosition(positionMs);
            } else {
                colorReplay.stop();
            }
        }
    }

    public void nextTrackPlaying(TrackWrapper trackWrapper) {
        analyzer.reset();
        byte[] musicColors = DbManager.getMusicColors(trackWrapper.getId());

        if (musicColors == null) {
            useAotData = false;
            Log.message("using JIT color data");
        } else {
            colorReplay.newTrack(musicColors);
            Log.message("using AOT color data");
        }
    }

    public void stop() {
        analyzer.reset();
    }

    @Override
    public void newSamples(short[] samples) {
        if (!useAotData && isEnabled) {
            analyzer.pushSamples(samples);
        }
    }

    @Override
    public void reportAudioParams(int sampleRate, float msPerFrame) {
        if (!useAotData) {
            analyzer.updateAudioParams(sampleRate, msPerFrame);
        }
    }

    @Override
    public void playbackStarted(int positionMs) {
        //adjust to playposition every time
        if (useAotData) {
            colorReplay.playFromPosition(positionMs);
        }
    }

    @Override
    public void playbackFinished() {
        //prevent ghost colors after finished playback
        colorReplay.stop();
    }

    @Override
    public void playbackStopped() {
        colorReplay.stop();
    }

    @Override
    public void playbackPaused() {
        colorReplay.stop();
    }

    @Override
    public void newResults(List<Map.Entry<Double, Double>> list) {
        if (isEnabled) {
            Color color = ColorTranslator.translatePartition2(list);
            listener.newColor(color);
        }
    }
}
