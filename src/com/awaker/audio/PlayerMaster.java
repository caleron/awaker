package com.awaker.audio;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.config.Config;
import com.awaker.config.ConfigKey;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.global.*;
import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;
import com.awaker.util.Log;
import javazoom.jl.decoder.JavaLayerException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerMaster implements PlayerListener, CommandHandler, EventReceiver {
    private static PlayerMaster instance = null;
    private TrackQueue trackQueue;

    private CustomPlayer player;
    private final FFTAnalyzer analyzer;
    private boolean customColorMode = false;

    private int volume = 100;

    /**
     * Erstellt eine neue Instanz. Es darf nur eine einzige Instanz existieren. Falls der Konstruktor ein zweites Mal
     * aufgerufen wird, wird eine {@link RuntimeException} geworfen.
     *
     * @param analyzeResultListener Der Listener für die Ergebnisse der Analyse mit FFT
     */
    public PlayerMaster(AnalyzeResultListener analyzeResultListener) {
        //Nur eine Instanz erlauben, damit nicht 2 Songs gleichzeitig abgespielt werden können.
        if (instance != null) {
            throw new RuntimeException("PlayerMaster already existing");
        }
        instance = this;

        analyzer = new FFTAnalyzer(analyzeResultListener);

        CommandRouter.registerHandler(AudioCommand.class, this);
        EventRouter.registerReceiver(this, GlobalEvent.SHUTDOWN);
        EventRouter.registerReceiver(this, GlobalEvent.MEDIA_READY);
    }

    public static PlayerMaster getInstance() {
        return instance;
    }

    @Override
    public Answer handleCommand(Command command, CommandData data) {
        if (!(command instanceof AudioCommand)) {
            throw new RuntimeException("Received Wrong Command");
        }

        AudioCommand cmd = (AudioCommand) command;

        switch (cmd) {
            case PLAY:
                play();
                break;
            case PLAY_ID:
                play(data.trackId);
                break;
            case PLAY_ID_LIST:
                playIdList(data.trackId, data.idList);
                break;
            case PLAY_FROM_POSITION:
                playFromPosition(data.position);
                break;
            case PAUSE:
                pause();
                break;
            case STOP:
                stop();
                break;
            case TOGGLE_PLAY_PAUSE:
                togglePlayPause();
                break;
            case PLAY_NEXT:
                playNext();
                break;
            case PLAY_PREVIOUS:
                playPrevious();
                break;
            case SET_SHUFFLE:
                Config.set(ConfigKey.SHUFFLE, data.shuffle);
                break;
            case SET_REPEAT_MODE:
                Config.set(ConfigKey.REPEAT_MODE, data.repeatMode);
                break;
            case SET_VOLUME:
                setVolume(data.volume);
                break;
            case PLAY_PLAYLIST:
                playPlaylist(data.playlistId);
                break;
            case PLAY_TRACK_OF_PLAYLIST:
                playTrackOfPlaylist(data.playlistId, data.trackId);
                break;
            case ADD_TRACKS_TO_QUEUE:
                addTracksToQueue(data.idList);
                break;
            case REMOVE_TRACKS_FROM_QUEUE:
                removeTracksFromQueue(data.idList);
                break;
            case PLAY_TRACK_OF_QUEUE:
                playTrackOfQueue(data.trackId);
                break;
            case PLAY_TRACK_NEXT:
                playTrackNext(data.trackId);
                break;
        }
        return Answer.status();
    }

    @Override
    public void receiveGlobalEvent(GlobalEvent globalEvent) {
        switch (globalEvent) {
            case SHUTDOWN:
                stop();
                break;
            case MEDIA_READY:
                trackQueue = TrackQueue.getInstance();
                break;
        }
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
    private synchronized boolean playFromPosition(int position) {
        return playCurrentTrack(position);
    }

    /**
     * Spielt einen Track aus einer Playlist ab und setzt die Playlist als aktive Playlist fest.
     *
     * @param playListId Die ID der Playlist mit dem Track.
     * @param trackId    Die Track-ID des abzuspielenden Tracks
     * @return False, falls playList oder track null sind.
     */
    private synchronized boolean playTrackOfPlaylist(int playListId, int trackId) {
        trackQueue.setPlaylist(playListId);
        trackQueue.setCurrentTrack(trackId);

        return playCurrentTrack();
    }

    /**
     * Spielt einen Track aus der Queue ab.
     *
     * @param trackId Die ID des Tracks
     * @return False, falls track null ist.
     */
    private synchronized boolean playTrackOfQueue(int trackId) {
        trackQueue.setCurrentTrack(trackId);

        return playCurrentTrack();
    }

    /**
     * Spielt einen Track aus der All-PlayList ab.
     *
     * @param trackId Die Track-ID des abzuspieldenden Tracks
     * @return True, wenn abgespielt wird
     */
    private synchronized boolean play(int trackId) {
        return playTrackOfPlaylist(-1, trackId);
    }

    /**
     * Spielt eine Playlist ab und setzt sie als aktiv.
     *
     * @return False, falls die playList null ist.
     */
    private synchronized boolean playPlaylist(int playListId, int firstId) {
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
    private synchronized boolean playPlaylist(int playListId) {
        return playPlaylist(playListId, -1);
    }

    private synchronized boolean playIdList(Integer playNowId, Integer[] list) {
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
    private synchronized void play() {
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
    private synchronized void playNext() {
        trackQueue.nextTrack();
        playCurrentTrack();
    }

    /**
     * Spielt den vorigen Song in der Playlist ab.
     */
    private synchronized void playPrevious() {
        trackQueue.previousTrack();
        playCurrentTrack();
    }

    /**
     * Wechselt zwischen Play und Pause.
     */
    private synchronized void togglePlayPause() {
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
    private synchronized void addTracksToQueue(Integer[] list) {
        for (Integer trackId : list) {
            trackQueue.addToQueue(trackId);
        }
    }

    /**
     * Entfernt eine Trackmenge aus der Warteschlange.
     *
     * @param list Array von Track-IDs
     */
    private synchronized void removeTracksFromQueue(Integer[] list) {
        for (Integer trackId : list) {
            trackQueue.removeFromQueue(trackId);
        }
    }

    /**
     * Fügt den Track zur ID als nächstes in die Warteschlange ein.
     *
     * @param id Die ID des Tracks.
     */
    private synchronized void playTrackNext(int id) {
        trackQueue.playAsNext(id);
    }

    /**
     * Pausiert die Wiedergabe
     */
    private synchronized void pause() {
        if (player != null) {
            player.pause();
            analyzer.reset();
        }
        EventRouter.raiseEvent(GlobalEvent.PLAYBACK_PAUSED);
    }

    /**
     * Stoppt die Wiedergabe
     */
    private synchronized void stop() {
        if (player != null) {
            player.stop();
        }
        EventRouter.raiseEvent(GlobalEvent.PLAYBACK_PAUSED);
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
    public Answer writeStatus(Answer answer) {
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

    private synchronized void setVolume(int newVolume) {
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
        EventRouter.raiseEvent(GlobalEvent.PLAYBACK_NEW_SONG);
    }

    @Override
    public void playbackStopped() {

    }

    @Override
    public void playbackPaused() {

    }

}
