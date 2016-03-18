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

    PlaybackListener playbackListener;

    /**
     * Erstellt eine neue Instanz
     *
     * @param resultListener Der Listener für die Ergebnisse der Analyse mit FFT
     */
    public PlayerMaster(PlaybackListener playbackListener, ResultListener resultListener) {
        analyzer = new FFTAnalyzer(resultListener);
        this.playbackListener = playbackListener;
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
     * Startet die Wiedergabe von einer bestimmten Position
     *
     * @param position Die Position in Sekunden
     * @return false, wenn die Datei nicht gefunden wurde
     */
    public boolean playFromPosition(int position) {
        FileInputStream fis = MediaManager.getFileStream(currentPlayList.getCurrentTrack());

        if (fis != null) {
            if (player != null) {
                player.stop();
            }
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
     * Startet die Wiedergabe. wählt den nächsten Song aus, falls die Wiedergabe gestoppt wurde.
     */
    public void play() {
        if (player == null || player.getStatus() == PlaybackStatus.STOPPED) {
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
        if (player != null && player.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Pausiert die Wiedergabe
     */
    public void pause() {
        if (player != null) {
            player.pause();
        }
        playbackListener.playbackPaused();
    }

    /**
     * Stoppt die Wiedergabe
     */
    public void stop() {
        if (player != null) {
            player.stop();
        }
        playbackListener.playbackPaused();
    }

    public void setShuffle(boolean shuffle) {
        currentPlayList.setShuffle(shuffle);
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        currentPlayList.setRepeatMode(repeatMode);
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();

        sb.append("playing:");
        if (player != null) {
            sb.append(String.valueOf(player.isPlaying()));
        } else {
            sb.append("false");
        }

        sb.append(";shuffle:");
        sb.append(String.valueOf(currentPlayList.isShuffle()));

        sb.append(";repeat:");
        if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_ALL) {
            sb.append("2");
        } else if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_FILE) {
            sb.append("1");
        } else {
            sb.append("0");
        }
        TrackWrapper currentTrack = currentPlayList.getCurrentTrack();
        if (currentTrack != null) {
            if (currentTrack.title.length() > 0) {
                sb.append(";currentTitle:");
                sb.append(currentTrack.title);
            }
            if (currentTrack.artist != null && currentTrack.artist.length() > 0) {
                sb.append(";currentArtist:");
                sb.append(currentTrack.artist);
            }
            if (currentTrack.album != null && currentTrack.album.length() > 0) {
                sb.append(";currentAlbum:");
                sb.append(currentTrack.album);
            }

            sb.append(";trackLength:");
            sb.append(currentTrack.trackLength);

            sb.append(";playPosition:");
            sb.append((int) (player.getPosition() / 1000.0));
        }

        return sb.toString();
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
