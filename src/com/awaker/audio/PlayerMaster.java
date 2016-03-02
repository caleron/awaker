package com.awaker.audio;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.analyzer.ResultListener;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;

public class PlayerMaster implements PlayerListener {
    PlayList currentPlayList = PlayList.ALL_TRACKS;

    CustomPlayer player;

    FFTAnalyzer analyzer;

    /**
     * Erstellt eine neue Instanz
     *
     * @param resultListener Der Listener für die Ergebnisse der Analyse mit FFT
     */
    public PlayerMaster(ResultListener resultListener) {
        analyzer = new FFTAnalyzer(resultListener);
    }

    /**
     * Spielt einen Track ab.
     *
     * @param track Der abzuspielende Track
     * @return false, wenn die Datei nicht gefunden wurde
     */
    public boolean playFile(TrackWrapper track) {
        FileInputStream fis = MediaManager.getFileStream(track);

        if (fis != null) {
            currentPlayList.setCurrentTrack(track);

            if (player != null) {
                player.stop();
            }
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

    /**
     * Startet die Wiedergabe. wählt den nächsten Song aus, falls die Wiedergabe gestoppt wurde.
     */
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

    /**
     * Startet die Wiedergabe von einer bestimmten Position
     *
     * @param position Die Position in Sekunden
     * @return false, wenn die Datei nicht gefunden wurde
     */
    public boolean playFromPosition(int position) {
        FileInputStream fis = MediaManager.getFileStream(currentPlayList.getCurrentTrack());

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

    /**
     * Spielt die nächste Datei in der Playlist ab.
     */
    public void playNext() {
        playFile(currentPlayList.nextTrack());
    }

    /**
     * Spielt den vorigen Song in der Playlist ab.
     */
    public void playPrevious() {
        playFile(currentPlayList.previousTrack());
    }

    /**
     * Wechselt zwischen Play und Pause.
     */
    public void tooglePlayPause() {
        if (player.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Pausiert die Wiedergabe
     */
    public void pause() {
        player.pause();
    }

    /**
     * Stoppt die Wiedergabe
     */
    public void stop() {
        player.stop();
    }

    public void setShuffle(boolean shuffle) {
        currentPlayList.setShuffle(shuffle);
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        currentPlayList.setRepeatMode(repeatMode);
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
        playNext();
    }

    @Override
    public void playbackStopped() {

    }

    @Override
    public void playbackPaused() {

    }

}
