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

    private String action;

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
            case "play":
                listener.play();
                break;

            case "playFromPosition":
                listener.playFromPosition(position);
                break;

            case "pause":
                listener.pause();
                break;

            case "stop":
                listener.stop();
                break;

            case "togglePlayPause":
                listener.togglePlayPause();
                break;

            case "playFile":
                if (!listener.playFile(new TrackWrapper(title, artist))) {
                    return Answer.fileNotFound();
                }
                break;

            case "uploadAndPlayFile":
                //abspielen
                listener.downloadFile(socketIn, length, fileName, true);
                break;

            case "checkFile":
                if (!listener.containsFile(new TrackWrapper(title, artist))) {
                    return Answer.fileNotFound();
                }
                break;

            case "uploadFile":
                //herunterladen
                listener.downloadFile(socketIn, length, fileName, false);
                break;

            case "playNext":
                listener.playNext();
                break;

            case "playPrevious":
                listener.playPrevious();
                break;

            case "setShuffle":
                listener.setShuffle(shuffle);
                break;

            case "setRepeatMode":
                listener.setRepeatMode(repeatMode);
                break;

            case "setVolume":
                listener.setVolume(volume);
                break;

            case "setWhiteBrightness":
                listener.setWhiteBrightness(brightness);
                break;

            case "setColorBrightness":
                listener.setColorBrightness(brightness);
                break;

            case "setColorMode":
                listener.setColorMode(colorMode);
                break;

            case "setColor":
                listener.setColor(new Color(color, false));
                break;

            case "setRGBColor":
                listener.setColor(new Color(red, green, blue));
                break;

            case "changeVisualization":
                listener.changeVisualisation(visualisation);
                break;

            case "getStatus":
                //Status wird sowieso ausgegeben
                break;

            case "getLibrary":
                return getLibrary(Answer.library());

            case "sendString":
                listener.stringReceived(text);
                break;
            case "shutdown":
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
