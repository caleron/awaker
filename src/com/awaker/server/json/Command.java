package com.awaker.server.json;

import com.awaker.config.Config;
import com.awaker.config.ConfigKey;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.ServerListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Repräsentiert einen Befehl, der von einem Client geschickt wurde. Beinhaltet alle möglichen Felder, die durch
 * Deserialisierung von JSON durch Gson gesetzt werden.
 */
@SuppressWarnings("unused")
public class Command {
    //TODO commands können auch vom server zum client gesendet werden, etwa beim Hinzufügen eines Tracks zu einer Playlist,
    //damit nicht zu jedem Client die gesamte Library geschickt werden muss

    private static final String PLAY = "play";
    private static final String PLAY_ID = "playId";
    private static final String PLAY_ID_LIST = "playIdList";
    private static final String PLAY_FROM_POSITION = "playFromPosition";
    private static final String PAUSE = "pause";
    private static final String STOP = "stop";
    private static final String TOGGLE_PLAY_PAUSE = "togglePlayPause";
    private static final String CHECK_FILE = "checkFile";
    private static final String PLAY_NEXT = "playNext";
    private static final String PLAY_PREVIOUS = "playPrevious";
    private static final String SET_SHUFFLE = "setShuffle";
    private static final String SET_REPEAT_MODE = "setRepeatMode";
    private static final String SET_VOLUME = "setVolume";
    private static final String SET_WHITE_BRIGHTNESS = "setWhiteBrightness";
    private static final String SET_ANIMATION_BRIGHTNESS = "setAnimationBrightness";
    private static final String SET_COLOR_MODE = "setColorMode";
    private static final String SET_COLOR = "setColor";
    private static final String SET_RGBCOLOR = "setRGBColor";
    private static final String CHANGE_VISUALIZATION = "changeVisualization";
    private static final String CREATE_PLAYLIST = "createPlaylist";
    private static final String REMOVE_PLAYLIST = "removePlaylist";
    private static final String ADD_TRACKS_TO_PLAYLIST = "addTracksToPlaylist";
    private static final String REMOVE_TRACKS_FROM_PLAYLIST = "removeTracksFromPlaylist";
    private static final String PLAY_PLAYLIST = "playPlaylist";
    private static final String PLAY_TRACK_OF_PLAYLIST = "playTrackOfPlaylist";
    private static final String ADD_TRACKS_TO_QUEUE = "addTracksToQueue";
    private static final String REMOVE_TRACKS_FROM_QUEUE = "removeTracksFromQueue";
    private static final String PLAY_TRACK_NEXT = "playTrackNext";
    private static final String GET_STATUS = "getStatus";
    private static final String GET_LIBRARY = "getLibrary";
    private static final String SEND_STRING = "sendString";
    private static final String SHUTDOWN_SERVER = "shutdownServer";
    private static final String SHUTDOWN_RASPI = "shutdownRaspi";
    private static final String REBOOT_RASPI = "rebootRaspi";
    private static final String REBOOT_SERVER = "rebootServer";
    private static final String GET_CONFIG = "getConfig";
    private static final String SET_CONFIG = "setConfig";
    private static final String GET_CONFIG_LIST = "getConfigList";
    private static final String GET_CONFIG_OPTIONS = "getConfigOptions";

    private String action;

    private String name;
    private String value;
    private Integer playlistId;
    private Integer trackId;
    private Integer[] idList;

    private String title;
    private String artist;
    private String fileName;

    private Integer position;
    private Integer length;
    private String repeatMode;

    private Integer red;
    private Integer green;
    private Integer blue;
    private Boolean smooth;

    private Boolean shuffle;
    private Integer volume;
    private Integer brightness;
    private Integer color;

    private String colorMode;
    private String visualisation;
    private String text;

    public Command() {
    }

    public Answer execute(ServerListener listener) throws Exceptions.CloseSocket, Exceptions.ShutdownServer, Exceptions.ShutdownRaspi, Exceptions.RebootServer, Exceptions.RebootRaspi {
        boolean returnLibrary = false;

        switch (action) {
            case PLAY:
                listener.play();
                break;

            case PLAY_ID:
                listener.play(trackId);
                break;

            case PLAY_ID_LIST:
                listener.playIdList(trackId, idList);
                break;

            case PLAY_FROM_POSITION:
                listener.playFromPosition(position);
                break;

            case PAUSE:
                listener.pause();
                break;

            case STOP:
                listener.stop();
                break;

            case TOGGLE_PLAY_PAUSE:
                listener.togglePlayPause();
                break;

            case CHECK_FILE:
                if (!listener.containsFile(new TrackWrapper(title, artist))) {
                    return Answer.fileNotFound();
                }
                break;

            case PLAY_NEXT:
                listener.playNext();
                break;

            case PLAY_PREVIOUS:
                listener.playPrevious();
                break;

            case SET_SHUFFLE:
                listener.setShuffle(shuffle);
                break;

            case SET_REPEAT_MODE:
                listener.setRepeatMode(repeatMode);
                break;

            case SET_VOLUME:
                listener.setVolume(volume);
                break;

            case SET_WHITE_BRIGHTNESS:
                listener.setWhiteBrightness(brightness, smooth);
                break;

            case SET_ANIMATION_BRIGHTNESS:
                listener.setAnimationBrightness(brightness, smooth);
                break;

            case SET_COLOR_MODE:
                listener.setColorMode(colorMode);
                break;

            case SET_COLOR:
                listener.setColor(new Color(color, false), smooth);
                break;

            case SET_RGBCOLOR:
                listener.setColor(new Color(red, green, blue), smooth);
                break;

            case CHANGE_VISUALIZATION:
                listener.changeVisualisation(visualisation);
                break;

            case CREATE_PLAYLIST:
                listener.createPlaylist(name);
                returnLibrary = true;
                break;

            case REMOVE_PLAYLIST:
                listener.removePlaylist(playlistId);
                returnLibrary = true;
                break;

            case ADD_TRACKS_TO_PLAYLIST:
                listener.addTracksToPlaylist(playlistId, idList);
                returnLibrary = true;
                break;

            case REMOVE_TRACKS_FROM_PLAYLIST:
                listener.removeTracksFromPlaylist(playlistId, idList);
                returnLibrary = true;
                break;

            case PLAY_PLAYLIST:
                listener.playPlaylist(playlistId);
                break;

            case PLAY_TRACK_OF_PLAYLIST:
                listener.playTrackOfPlaylist(playlistId, trackId);
                break;

            case PLAY_TRACK_NEXT:
                listener.playTrackNext(trackId);
                break;

            case ADD_TRACKS_TO_QUEUE:
                listener.addTracksToQueue(idList);
                returnLibrary = true;
                break;

            case REMOVE_TRACKS_FROM_QUEUE:
                listener.removeTracksFromQueue(idList);
                returnLibrary = true;
                break;

            case GET_STATUS:
                //Status wird sowieso ausgegeben
                break;

            case GET_LIBRARY:
                returnLibrary = true;
                break;

            case SEND_STRING:
                listener.stringReceived(text);
                break;

            case GET_CONFIG:
                Answer answer = Answer.config();
                answer.name = name;
                answer.value = Config.getString(ConfigKey.getForKey(name));
                return answer;

            case SET_CONFIG:
                answer = Answer.config();
                answer.name = name;
                ConfigKey key = ConfigKey.getForKey(name);
                Config.set(key, value);
                answer.value = Config.getString(key);
                return answer;

            case GET_CONFIG_LIST:
                answer = Answer.config();
                answer.config = Config.getConfig();
                return answer;

            case GET_CONFIG_OPTIONS:
                answer = Answer.config();
                answer.configOptions = Config.getConfigOptions();
                return answer;

            case SHUTDOWN_SERVER:
                throw new Exceptions.ShutdownServer();
            case SHUTDOWN_RASPI:
                throw new Exceptions.ShutdownRaspi();
            case REBOOT_RASPI:
                throw new Exceptions.RebootRaspi();
            case REBOOT_SERVER:
                throw new Exceptions.RebootServer();
            default:
                throw new Exceptions.CloseSocket();
        }

        if (returnLibrary) {
            return getLibrary(listener.getStatus(Answer.library()));
        }
        return listener.getStatus(Answer.status());
    }

    /**
     * Gibt die Mediathek zurück.
     *
     * @param library Die Mediathek als Answer
     * @return Das modifizierte Answer-Objekt
     */
    private static Answer getLibrary(Answer library) {
        library.tracks = new ArrayList<>();
        ArrayList<TrackWrapper> allTracks = MediaManager.getAllTracks();

        library.tracks.addAll(allTracks.stream()
                .map(track -> new Track(track.getId(), track.title, track.artist, track.album, track.trackLength))
                .collect(Collectors.toList()));

        library.playLists = MediaManager.getPlayListsForJson();

        return library;
    }
}
