package com.awaker.audio;

import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Playlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class PlayList {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TABLE_NAME = "playlists";
    public static final String PLAYLIST_TRACKS_TABLE_NAME = "playlist_tracks";
    public static final String PLAYLIST_TRACKS_PLAYLIST_ID = "playlist_id";
    public static final String PLAYLIST_TRACKS_TRACK_ID = "track_id";

    public static final PlayList ALL_TRACKS = new PlayList(-1, "Alle", null);

    private final int id;
    private final Random rand = new Random();
    private final String name;

    /**
     * Wenn tracks == null, dann umfasst die Playlist alle Tracks
     */
    private List<TrackWrapper> tracks;

    private TrackWrapper currentTrack;
    private boolean shuffle = true;
    private RepeatMode repeatMode = RepeatMode.REPEAT_MODE_ALL;

    private final Stack<TrackWrapper> trackHistory = new Stack<>();

    /**
     * Erstellt eine neue Playlist
     *
     * @param id     ID der Playlist
     * @param name   Name der Playlist
     * @param tracks Liste der Tracks
     */
    public PlayList(int id, String name, ArrayList<TrackWrapper> tracks) {
        this.id = id;
        this.name = name;
        this.tracks = tracks;
    }

    /**
     * Erstellt eine neue Playlist mit einer leeren Trackliste.
     *
     * @param id   ID der Playlist
     * @param name Name der Playlist
     */
    public PlayList(int id, String name) {
        this.id = id;
        this.name = name;
        this.tracks = new ArrayList<>();
    }

    /**
     * Erstellt eine neue Playlist mit einer leeren Trackliste und ohne id.
     *
     * @param name Name der Playlist
     */
    public PlayList(String name) {
        id = -1;
        this.name = name;
    }

    /**
     * Wählt den nächsten Track aus
     *
     * @return der nächste Track
     */
    public TrackWrapper nextTrack() {
        if (tracks == null) {
            tracks = MediaManager.getAllTracks();
        }

        if (tracks.size() == 0)
            return null;

        //index des aktuellen Tracks bestimmen
        int currentTrackIndex;
        if (currentTrack == null) {
            currentTrackIndex = -1;
        } else {
            currentTrackIndex = tracks.indexOf(currentTrack);
        }

        if (repeatMode == RepeatMode.REPEAT_MODE_FILE && currentTrackIndex >= 0) {
            //aktuellen Track wiederholen
            return currentTrack;
        } else if (currentTrackIndex == -1 || shuffle) {
            //Zufälligen Track auswählen
            int nextIndex = Math.round(rand.nextFloat() * (tracks.size() - 1));
            currentTrack = tracks.get(nextIndex);

            trackHistory.add(currentTrack);
        } else {
            //nächsten oder ersten Track auswählen
            if (currentTrackIndex + 1 < tracks.size()) {
                currentTrack = tracks.get(currentTrackIndex + 1);
            } else {
                currentTrack = tracks.get(0);
            }
            trackHistory.add(currentTrack);
        }
        return currentTrack;
    }

    /**
     * Gibt den vorherigen Track wieder
     *
     * @return Der vorherige Track
     */
    public TrackWrapper previousTrack() {
        if (currentTrack == null || trackHistory.size() < 2) {
            return nextTrack();
        } else {
            trackHistory.pop();
            currentTrack = trackHistory.peek();
            return currentTrack;
        }
    }

    /**
     * Fügt einen Track zur Playlist hinzu.
     *
     * @param track Der neue Track.
     */
    public void addTrack(TrackWrapper track) {
        tracks.add(track);
    }

    /**
     * Entfernt einen Track von der Playlist.
     *
     * @param track Der zu entfernende Track.
     */
    public void removeTrack(TrackWrapper track) {
        tracks.remove(track);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public List<TrackWrapper> getTracks() {
        return tracks;
    }

    public TrackWrapper getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(int id) {
        for (TrackWrapper track : tracks) {
            if (track.getId() == id) {
                currentTrack = track;
                break;
            }
        }
    }

    public void setCurrentTrack(TrackWrapper currentTrack) {
        this.currentTrack = currentTrack;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public String getInsertSQL() {
        return String.format("INSERT INTO playlists (name) VALUES (\"%s\")", name);
    }

    public Playlist toJSONPlaylist() {
        ArrayList<Integer> idList = new ArrayList<>();

        for (TrackWrapper track : tracks) {
            idList.add(track.getId());
        }

        return new Playlist(id, name, idList);
    }

    public static String getCreateTableSql() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" INTEGER PRIMARY KEY, " +
                "\"%s\" TEXT)", TABLE_NAME, ID, NAME);
    }

    public static String getCreatePlaylistTracksTableSql() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" INTEGER PRIMARY KEY, " +
                "\"%s\" INTEGER," +
                "\"%s\" INTEGER)", PLAYLIST_TRACKS_TABLE_NAME, ID, PLAYLIST_TRACKS_PLAYLIST_ID, PLAYLIST_TRACKS_TRACK_ID);
    }

    //TODO machen
    public void addToQueue(TrackWrapper track) {
        tracks.add(track);
    }

    //TODO machen
    public void playNext(TrackWrapper track) {
        tracks.add(track);
    }
}
