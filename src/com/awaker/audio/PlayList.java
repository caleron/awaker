package com.awaker.audio;

import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlayList {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TABLE_NAME = "playlists";
    public static final String PLAYLIST_TRACKS_TABLE_NAME = "playlist_tracks";
    public static final String PLAYLIST_TRACKS_PLAYLIST_ID = "playlist_id";
    public static final String PLAYLIST_TRACKS_TRACK_ID = "track_id";

    private final int id;
    private final String name;

    private ArrayList<TrackWrapper> tracks;

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
     * Fügt einen Track zur Playlist hinzu.
     *
     * @param track Der neue Track.
     * @return False, wenn der Track bereits in der Playlist ist.
     */
    public boolean addTrack(TrackWrapper track) {
        if (hasTrack(track))
            return false;

        tracks.add(track);
        return true;
    }

    /**
     * Entfernt einen Track von der Playlist.
     *
     * @param track Der zu entfernende Track.
     */
    public void removeTrack(TrackWrapper track) {
        tracks.remove(track);
    }

    /**
     * Sucht den Track in der TrackListe
     *
     * @param track Der zu suchende Track
     * @return True, wenn der Track zur Playlist gehört.
     */
    private boolean hasTrack(TrackWrapper track) {
        for (TrackWrapper trackWrapper : tracks) {
            if (trackWrapper.getId() == track.getId())
                return true;
        }
        return false;
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

    /**
     * Generiert ein Objekt für die JSON-Repräsentation dieser Playlist.
     *
     * @return Playlist-Objekt
     */
    public Playlist toJSONPlaylist() {
        List<Integer> idList = new ArrayList<>();
        for (TrackWrapper track : tracks) {
            idList.add(track.getId());
        }
        return new Playlist(id, name, idList);
    }

    public String getInsertSQL() {
        return String.format("INSERT INTO playlists (name) VALUES (\"%s\")", name);
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

}
