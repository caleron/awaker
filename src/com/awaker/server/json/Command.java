package com.awaker.server.json;

import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.ServerListener;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Repräsentiert einen Befehl, der von einem Client geschickt wurde. Beinhaltet alle möglichen Felder, die durch
 * Deserialisierung von JSON durch Gson gesetzt werden.
 */
@SuppressWarnings("unused")
public class Command {

    private static final String PLAY = "play";
    private static final String PLAY_FROM_POSITION = "playFromPosition";
    private static final String PAUSE = "pause";
    private static final String STOP = "stop";
    private static final String TOGGLE_PLAY_PAUSE = "togglePlayPause";
    private static final String PLAY_FILE = "playFile";
    private static final String UPLOAD_AND_PLAY_FILE = "uploadAndPlayFile";
    private static final String CHECK_FILE = "checkFile";
    private static final String UPLOAD_FILE = "uploadFile";
    private static final String PLAY_NEXT = "playNext";
    private static final String PLAY_PREVIOUS = "playPrevious";
    private static final String SET_SHUFFLE = "setShuffle";
    private static final String SET_REPEAT_MODE = "setRepeatMode";
    private static final String SET_VOLUME = "setVolume";
    private static final String SET_WHITE_BRIGHTNESS = "setWhiteBrightness";
    private static final String SET_COLOR_BRIGHTNESS = "setColorBrightness";
    private static final String SET_COLOR_MODE = "setColorMode";
    private static final String SET_COLOR = "setColor";
    private static final String SET_RGBCOLOR = "setRGBColor";
    private static final String CHANGE_VISUALIZATION = "changeVisualization";
    private static final String CREATE_PLAYLIST = "createPlaylist";
    private static final String REMOVE_PLAYLIST = "removePlaylist";
    private static final String ADD_TRACK_TO_PLAYLIST = "addTrackToPlaylist";
    private static final String REMOVE_TRACK_FROM_PLAYLIST = "removeTrackFromPlaylist";
    private static final String PLAY_PLAYLIST = "playPlaylist";
    private static final String PLAY_TRACK_OF_PLAYLIST = "playTrackOfPlaylist";
    private static final String GET_STATUS = "getStatus";
    private static final String GET_LIBRARY = "getLibrary";
    private static final String SEND_STRING = "sendString";
    private static final String SHUTDOWN = "shutdown";

    private String action;

    private String name;
    private int playlistId;
    private int trackId;

    private String title;
    private String artist;
    private String fileName;

    private int position;
    private int length;
    private int repeatMode;

    private int red;
    private int green;
    private int blue;

    private boolean shuffle;
    private int volume;
    private int brightness;
    private int color;

    private String colorMode;
    private String visualisation;
    private String text;

    public Command() {
    }

    public Answer execute(ServerListener listener, InputStream socketIn) throws Exceptions.CloseSocket, Exceptions.Shutdown {

        switch (action) {
            case PLAY:
                listener.play();
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

            case PLAY_FILE:
                if (!listener.playFile(new TrackWrapper(title, artist))) {
                    return Answer.fileNotFound();
                }
                break;

            case UPLOAD_AND_PLAY_FILE:
                //abspielen
                listener.downloadFile(socketIn, length, fileName, true);
                break;

            case CHECK_FILE:
                if (!listener.containsFile(new TrackWrapper(title, artist))) {
                    return Answer.fileNotFound();
                }
                break;

            case UPLOAD_FILE:
                //herunterladen
                listener.downloadFile(socketIn, length, fileName, false);
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
                listener.setWhiteBrightness(brightness);
                break;

            case SET_COLOR_BRIGHTNESS:
                listener.setColorBrightness(brightness);
                break;

            case SET_COLOR_MODE:
                listener.setColorMode(colorMode);
                break;

            case SET_COLOR:
                listener.setColor(new Color(color, false));
                break;

            case SET_RGBCOLOR:
                listener.setColor(new Color(red, green, blue));
                break;

            case CHANGE_VISUALIZATION:
                listener.changeVisualisation(visualisation);
                break;

            case CREATE_PLAYLIST:
                listener.createPlaylist(name);
                break;

            case REMOVE_PLAYLIST:
                listener.removePlaylist(playlistId);
                break;

            case ADD_TRACK_TO_PLAYLIST:
                listener.addTrackToPlaylist(playlistId, trackId);
                break;

            case REMOVE_TRACK_FROM_PLAYLIST:
                listener.removeTrackFromPlaylist(playlistId, trackId);
                break;

            case PLAY_PLAYLIST:
                listener.playPlaylist(playlistId);
                break;

            case PLAY_TRACK_OF_PLAYLIST:
                listener.playTrackOfPlaylist(playlistId, trackId);
                break;

            case GET_STATUS:
                //Status wird sowieso ausgegeben
                break;

            case GET_LIBRARY:
                return getLibrary(Answer.library());

            case SEND_STRING:
                listener.stringReceived(text);
                break;
            case SHUTDOWN:
                throw new Exceptions.Shutdown();

            default:
                throw new Exceptions.CloseSocket();
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

        library.playlists = DbManager.getAllPlaylistsForJSON();

        return library;
    }
}