package com.awaker.audio;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.analyzer.ResultListener;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;

public class PlayerMaster implements PlayerListener {
    TrackWrapper currentTrack;
    RepeatMode repeatMode;
    boolean shuffle;
    PlayList currentPlayList = PlayList.ALL_TRACKS;

    CustomPlayer player;

    FFTAnalyzer analyzer;

    public PlayerMaster(ResultListener resultListener) {
        analyzer = new FFTAnalyzer(resultListener);
    }

    public boolean playFile(TrackWrapper track) {
        FileInputStream fis = MediaManager.getFileStream(track);

        if (fis != null) {
            currentTrack = track;

            player.stop();
            try {
                player = new CustomPlayer(this, fis);
                player.play();
                return true;
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void play() {
        if (player.getStatus() == PlaybackStatus.STOPPED) {
            playNext();
        } else {
            try {
                player.play();
            } catch (JavaLayerException e) {
                e.printStackTrace();
                //playNext();
            }
        }
    }

    public boolean playFromPosition(TrackWrapper track, int position) {
        FileInputStream fis = MediaManager.getFileStream(track);

        if (fis != null) {
            player.stop();
            try {
                player = new CustomPlayer(this, fis);
                player.playFromPosition(position);
                return true;
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void playNext() {

    }

    public void playPrevious() {

    }

    public void pause() {
        player.pause();
    }

    public void stop() {
        player.stop();
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public String getStatus() {
        return player.getStatus().toString();
    }

    @Override
    public void newSamples(short[] samples) {
        analyzer.pushSamples(samples);
    }

    @Override
    public void playbackStarted() {

    }

    @Override
    public void playbackFinished() {

    }

    @Override
    public void playbackStopped() {

    }

    @Override
    public void playbackPaused() {

    }

    public void tooglePlayPause() {

    }
}
