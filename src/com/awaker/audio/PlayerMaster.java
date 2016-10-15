package com.awaker.audio;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.config.Config;
import com.awaker.config.ConfigKey;
import com.awaker.data.MediaEventListener;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Answer;
import com.awaker.util.Log;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerMaster implements PlayerListener, MediaEventListener {
    private static PlayerMaster instance = null;
    private TrackQueue trackQueue;

    private CustomPlayer player;
    private final FFTAnalyzer analyzer;
    private boolean customColorMode = false;

    private final PlaybackListener playbackListener;

    private int volume = 100;

    /**
     * Erstellt eine neue Instanz. Es darf nur eine einzige Instanz existieren. Falls der Konstruktor ein zweites Mal
     * aufgerufen wird, wird eine {@link RuntimeException} geworfen.
     *
     * @param analyzeResultListener Der Listener für die Ergebnisse der Analyse mit FFT
     */
    public PlayerMaster(PlaybackListener playbackListener, AnalyzeResultListener analyzeResultListener) {
        //Nur eine Instanz erlauben, damit nicht 2 Songs gleichzeitig abgespielt werden können.
        if (instance != null) {
            throw new RuntimeException("PlayerMaster already existing");
        }
        instance = this;

        analyzer = new FFTAnalyzer(analyzeResultListener);
        this.playbackListener = playbackListener;
        MediaManager.addListener(this);
    }

    @Override
    public void mediaReady() {
        trackQueue = TrackQueue.getInstance();
    }

    private synchronized boolean playCurrentTrack() {
        return playCurrentTrack(0);
    }

    /**
     * Spielt den aktuellen Track ab.
     *
     * @return false, wenn die Datei nicht gefunden wurde
     */
    private synchronized boolean playCurrentTrack(int position) {
        TrackWrapper track = trackQueue.currentTrack();

        if (track == null)
            return false;

        FileInputStream fis = MediaManager.getFileStream(track);

        analyzer.reset();

        if (fis != null) {
            if (player != null) {
                player.stop();
            }
            analyzer.reset();
            try {
                player = new CustomPlayer(this, fis, volume);
                if (position == 0) {
                    player.play();
                } else {
                    player.playFromPosition(position);
                }
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
    public synchronized boolean playFromPosition(int position) {
        return playCurrentTrack(position);
    }

    /**
     * Spielt einen Track aus einer Playlist ab und setzt die Playlist als aktive Playlist fest.
     *
     * @param playListId Die ID der Playlist mit dem Track.
     * @param trackId    Die Track-ID des abzuspielenden Tracks
     * @return False, falls playList oder track null sind.
     */
    public synchronized boolean playTrackOfPlaylist(int playListId, int trackId) {
        trackQueue.setPlaylist(playListId);
        trackQueue.setCurrentTrack(trackId);

        return playCurrentTrack();
    }

    /**
     * Spielt einen Track aus der All-PlayList ab.
     *
     * @param trackId Die Track-ID des abzuspieldenden Tracks
     * @return True, wenn abgespielt wird
     */
    public synchronized boolean play(int trackId) {
        return playTrackOfPlaylist(-1, trackId);
    }

    /**
     * Spielt eine Playlist ab und setzt sie als aktiv.
     *
     * @return False, falls die playList null ist.
     */
    public synchronized boolean playPlaylist(int playListId, int firstId) {
        trackQueue.setPlaylist(playListId);
        if (firstId >= 0) {
            trackQueue.setCurrentTrack(firstId);
            return playCurrentTrack();
        } else {
            playNext();
            return true;
        }
    }

    /**
     * Spielt eine Playlist ab und setzt sie als aktiv.
     *
     * @param playListId Die ID der Playlist.
     * @return False, falls die playList null ist.
     */
    public synchronized boolean playPlaylist(int playListId) {
        return playPlaylist(playListId, -1);
    }

    public synchronized boolean playIdList(Integer playNowId, Integer[] list) {
        List<Integer> idList = new ArrayList<>(Arrays.asList(list));
        ArrayList<TrackWrapper> allTracks = MediaManager.getAllTracks();
        ArrayList<TrackWrapper> tracks = new ArrayList<>();

        for (TrackWrapper track : allTracks) {
            if (idList.contains(track.getId())) {
                tracks.add(track);
            }
        }

        trackQueue.setTrackList(tracks);
        if (playNowId >= 0) {
            trackQueue.setCurrentTrack(playNowId);
        }
        return playCurrentTrack();
    }

    /**
     * Startet die Wiedergabe. wählt den nächsten Song aus, falls die Wiedergabe gestoppt wurde.
     */
    public synchronized void play() {
        if (player == null || player.getStatus() == PlaybackStatus.STOPPED) {
            playCurrentTrack();
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
    public synchronized void playNext() {
        trackQueue.nextTrack();
        playCurrentTrack();
    }

    /**
     * Spielt den vorigen Song in der Playlist ab.
     */
    public synchronized void playPrevious() {
        trackQueue.previousTrack();
        playCurrentTrack();
    }

    /**
     * Wechselt zwischen Play und Pause.
     */
    public synchronized void tooglePlayPause() {
        if (player != null && player.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Fügt der Warteschlange einen Track am Ende hinzu.
     *
     * @param list Array von Track-IDs
     */
    public synchronized void addTracksToQueue(Integer[] list) {
        for (Integer trackId : list) {
            trackQueue.addToQueue(trackId);
        }
    }

    /**
     * Entfernt eine Trackmenge aus der Warteschlange.
     *
     * @param list Array von Track-IDs
     */
    public synchronized void removeTracksFromQueue(Integer[] list) {
        for (Integer trackId : list) {
            trackQueue.removeFromQueue(trackId);
        }
    }

    /**
     * Fügt den Track zur ID als nächstes in die Warteschlange ein.
     *
     * @param id Die ID des Tracks.
     */
    public synchronized void playTrackNext(int id) {
        trackQueue.playAsNext(id);
    }

    /**
     * Pausiert die Wiedergabe
     */
    public synchronized void pause() {
        if (player != null) {
            player.pause();
            analyzer.reset();
        }
        playbackListener.playbackPaused();
    }

    /**
     * Stoppt die Wiedergabe
     */
    public synchronized void stop() {
        if (player != null) {
            player.stop();
        }
        playbackListener.playbackPaused();
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
        answer.shuffle = Config.getBool(ConfigKey.SHUFFLE);

        answer.repeatMode = Config.getString(ConfigKey.REPEAT_MODE);

        answer.volume = volume;

        if (trackQueue != null) {
            TrackWrapper currentTrack = trackQueue.currentTrack();
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

                if (player == null) {
                    answer.playPosition = 0;
                } else {
                    answer.playPosition = (int) (player.getPosition() / 1000.0);
                }
            }
            answer.trackQueue = trackQueue.toJSONPlaylist();
        }
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

    public synchronized void setVolume(int newVolume) {
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
}
