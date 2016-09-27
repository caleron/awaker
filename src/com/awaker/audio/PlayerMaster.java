package com.awaker.audio;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Answer;
import com.awaker.util.Log;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;

public class PlayerMaster implements PlayerListener {
    private PlayList currentPlayList = PlayList.ALL_TRACKS;

    private CustomPlayer player;
    private final FFTAnalyzer analyzer;
    private boolean customColorMode = false;

    private final PlaybackListener playbackListener;

    private int volume = 70;

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
            if (!currentPlayList.hasTrack(track)) {
                currentPlayList = PlayList.ALL_TRACKS;
            }
            currentPlayList.setCurrentTrack(track);

            if (player != null) {
                player.stop();
            }
            analyzer.reset();
            try {
                player = new CustomPlayer(this, fis, volume);
                player.play();
                return true;
            } catch (JavaLayerException e) {
                Log.error(e);
            }
        }
        return false;
    }

    /**
     * Spielt einen Track aus einer Playlist ab und setzt die Playlist als aktive Playlist fest.
     *
     * @param playList Die Playlist mit dem Track.
     * @param track    Der abzuspielende Track.
     * @return False, falls playList oder track null sind.
     */
    public boolean playTrackOfPlaylist(PlayList playList, TrackWrapper track) {
        if (playList == null || track == null)
            return false;

        currentPlayList = playList;
        currentPlayList.setCurrentTrack(track);
        playFile(track);
        return true;
    }

    /**
     * Spielt einen Track aus der All-PlayList ab.
     *
     * @param track Der Track
     */
    public boolean play(TrackWrapper track) {
        return playTrackOfPlaylist(PlayList.ALL_TRACKS, track);
    }

    /**
     * Spielt eine Playlist ab und setzt sie als aktiv.
     *
     * @return False, falls die playList null ist.
     */
    public boolean playPlaylist(PlayList playList, int firstId) {
        if (playList == null)
            return false;

        currentPlayList = playList;
        if (firstId >= 0) {
            currentPlayList.setCurrentTrack(firstId);
            return playFile(currentPlayList.getCurrentTrack());
        } else {
            playNext();
            return true;
        }
    }

    /**
     * Spielt eine Playlist ab und setzt sie als aktiv.
     *
     * @param playList Die Playlist.
     * @return False, falls die playList null ist.
     */
    public boolean playPlaylist(PlayList playList) {
        return playPlaylist(playList, -1);
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
                player = new CustomPlayer(this, fis, volume);
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

    /**
     * Schreibt den Status in das angegebene Answer-Objekt.
     *
     * @param answer Das zu modifizierende Answer-Objekt
     * @return Das Answer-Objekt
     */
    public Answer getStatus(Answer answer) {
        answer.playing = player != null && player.isPlaying();
        answer.shuffle = currentPlayList.isShuffle();

        if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_ALL) {
            answer.repeatMode = 2;
        } else if (currentPlayList.getRepeatMode() == RepeatMode.REPEAT_MODE_FILE) {
            answer.repeatMode = 1;
        } else {
            answer.repeatMode = 0;
        }

        answer.volume = volume;

        TrackWrapper currentTrack = currentPlayList.getCurrentTrack();
        if (currentTrack != null) {
            if (currentTrack.title.length() > 0) {
                answer.currentTitle = currentTrack.title;
            }
            if (currentTrack.artist != null && currentTrack.artist.length() > 0) {
                answer.currentArtist = currentTrack.artist;
            }
            if (currentTrack.album != null && currentTrack.album.length() > 0) {
                answer.currentAlbum = currentTrack.album;
            }
            answer.trackLength = currentTrack.trackLength;
            answer.currentTrackId = currentTrack.getId();

            answer.playPosition = (int) (player.getPosition() / 1000.0);
        }

        answer.currentPlaylist = currentPlayList.toJSONPlaylist();

        return answer;
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

    public void setVolume(int newVolume) {
        this.volume = Math.min(100, Math.max(newVolume, 0));
        if (player != null) {
            player.setVolume(volume);
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
        playbackListener.playbackNewSong();
    }

    @Override
    public void playbackStopped() {

    }

    @Override
    public void playbackPaused() {

    }

    public void addTrackToQueue(TrackWrapper track) {
        currentPlayList.addToQueue(track);
    }

    public void playTrackNext(TrackWrapper track) {
        currentPlayList.playNext(track);
    }
}
