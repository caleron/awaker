package com.awaker.audio;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.util.Log;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;

public class PlayerMaster implements PlayerListener {
    private PlayList currentPlayList = PlayList.ALL_TRACKS;

    private CustomPlayer player;
    private final FFTAnalyzer analyzer;
    private boolean customColorMode = false;

    private final PlaybackListener playbackListener;

    /**
     * Erstellt eine neue Instanz
     *
     * @param analyzeResultListener Der Listener für die Ergebnisse der Analyse mit FFT
     */
    public PlayerMaster(PlaybackListener playbackListener, AnalyzeResultListener analyzeResultListener) {
        analyzer = new FFTAnalyzer(analyzeResultListener);
        this.playbackListener = playbackListener;
    }

    /**
     * Spielt einen Track ab.
     *
     * @param track Der abzuspielende Track
     * @return false, wenn die Datei nicht gefunden wurde
     */
    public boolean playFile(TrackWrapper track) {
        if (track == null)
            return false;

        if (track.filePath == null || track.filePath.length() == 0) {
            //Track aus der Datenbank holen, falls der Wrapper vom Server erstellt wurde
            track = DbManager.getTrack(track.title, track.artist);
        }

        FileInputStream fis = MediaManager.getFileStream(track);

        analyzer.reset();

        if (fis != null) {
            currentPlayList.setCurrentTrack(track);

            if (player != null) {
                player.stop();
            }
            analyzer.reset();
            try {
                player = new CustomPlayer(this, fis);
                player.play();
                return true;
            } catch (JavaLayerException e) {
                Log.error(e);
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
            analyzer.reset();
            try {
                player = new CustomPlayer(this, fis);
                player.playFromPosition(position);
                return true;
            } catch (JavaLayerException e) {
                Log.error(e);
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
                Log.error(e);
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
            analyzer.reset();
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

    /**
     * Gibt die Position in Millisekunden zurück
     *
     * @return Position
     */
    private int getPosition() {
        return player.getPosition();
    }

    public void printPosition() {
        if (player == null)
            return;

        int sampleRate = player.getSampleRate();
        int analyzePosition = 0;

        if (sampleRate > 0) {
            analyzePosition = (int) ((analyzer.getAnalyzedSamplesCount() * 1.0) / (sampleRate / 1000.0)) + player.getOffsetPlayedMs();
        }

        Log.message("sampleRate: " + sampleRate
                + " playerPosition: " + getPosition()
                + " analyzePosition: " + analyzePosition
                + " difference: " + (getPosition() - analyzePosition));
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder(100);

        sb.append("playing:");
        if (player != null) {
            sb.append(String.valueOf(player.isPlaying()));
        } else {
            sb.append("false");
        }
        sb.append(";").append("shuffle:");

        sb.append(String.valueOf(currentPlayList.isShuffle())).append(";");

        sb.append("repeat:");
        if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_ALL) {
            sb.append("2");
        } else if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_FILE) {
            sb.append("1");
        } else {
            sb.append("0");
        }
        sb.append(";");

        TrackWrapper currentTrack = currentPlayList.getCurrentTrack();
        if (currentTrack != null) {
            if (currentTrack.title.length() > 0) {
                sb.append("currentTitle:");
                sb.append(currentTrack.title).append(";");
            }
            if (currentTrack.artist != null && currentTrack.artist.length() > 0) {
                sb.append("currentArtist:");
                sb.append(currentTrack.artist).append(";");
            }
            if (currentTrack.album != null && currentTrack.album.length() > 0) {
                sb.append("currentAlbum:");
                sb.append(currentTrack.album).append(";");
            }

            sb.append("trackLength:");
            sb.append(currentTrack.trackLength).append(";");

            sb.append("playPosition:");
            sb.append((int) (player.getPosition() / 1000.0)).append(";");
        }

        return sb.toString();
    }

    /**
     * Setzt den Beleuchtungsmodus
     *
     * @param customColorMode True, wenn Lichtfarbe manuell gesetzt wird
     */
    public void setColorMode(boolean customColorMode) {
        this.customColorMode = customColorMode;
    }

    @Override
    public void newSamples(short[] samples) {
        if (!customColorMode) {
            //Falls manuelle Lichtfarbe, keine Analyse auslösen
            analyzer.pushSamples(samples);
        }
    }

    @Override
    public void reportAudioParams(int sampleRate, float msPerFrame) {
        analyzer.updateAudioParams(sampleRate, msPerFrame);
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
