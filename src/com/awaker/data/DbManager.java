package com.awaker.data;

import com.awaker.audio.PlayList;
import com.awaker.server.json.Playlist;
import com.awaker.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbManager {
    private static Connection connection;
    private static final String DB_PATH = "media.sqlite";

    /**
     * http://www.tutorialspoint.com/sqlite/sqlite_java.htm
     */
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.error("Fehler beim Laden des JDBC-Treibers");
            Log.error(e);
        }
    }

    /**
     * Initialisiert die Datenbankverbindung und fügt einen ShutdownHook hinzu, um die Verbindung zu schließen.
     */
    public static void init() {
        try {
            if (connection != null)
                return;

            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

            if (!connection.isClosed()) {
                Log.message("Database Connection established");
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    Log.error(e);
                }
            }
        });
        setupDb();
    }

    /**
     * Erstellt die Datenbanktabellen (2)
     */
    private static void setupDb() {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(TrackWrapper.getCreateTableSql());
            statement.executeUpdate(PlayList.getCreateTableSql());
            statement.executeUpdate(PlayList.getCreatePlaylistTracksTableSql());
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Gibt alle Tracks zurück
     *
     * @return ArrayList mit allen Tracks
     */
    static ArrayList<TrackWrapper> getAllTracks() {
        try {
            ArrayList<TrackWrapper> res = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM music");

            while (resultSet.next()) {
                res.add(readTrack(resultSet));
            }

            resultSet.close();
            statement.close();
            return res;
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Gibt einen Track mit den angegebenen Tags zurück
     *
     * @param title  Der Titel
     * @param artist Der Künstler
     * @return der gesuchte Track
     */
    public static TrackWrapper getTrack(String title, String artist) {
        try {
            Statement statement = connection.createStatement();
            String sql;
            if (artist == null || (artist.length() == 0)) {
                sql = String.format("SELECT * FROM music WHERE title LIKE \"%s\" LIMIT 1", title);
            } else {
                sql = String.format("SELECT * FROM music WHERE artist LIKE \"%s\" AND title LIKE \"%s\" LIMIT 1", artist, title);
            }

            ResultSet resultSet = statement.executeQuery(sql);
            TrackWrapper track = null;

            if (resultSet.next()) {
                track = readTrack(resultSet);
            }

            resultSet.close();
            statement.close();
            return track;
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Liest einen Track aus dem ResultSet aus
     *
     * @param resultSet Das ResultSet mit dem Cursor auf dem auszulesenden Track
     * @return Der Track
     * @throws SQLException
     */
    private static TrackWrapper readTrack(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(TrackWrapper.ID);
        String title = resultSet.getString(TrackWrapper.TITLE);
        String artist = resultSet.getString(TrackWrapper.ARTIST);
        String album = resultSet.getString(TrackWrapper.ALBUM);
        String file = resultSet.getString(TrackWrapper.FILE_PATH);
        int length = resultSet.getInt(TrackWrapper.TRACK_LENGTH);

        return new TrackWrapper(id, title, artist, album, file, length);
    }

    /**
     * Fügt einen Track zur Datenbank hinzu
     *
     * @param track Der hinzuzufügende Track
     */
    static void addTrack(TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(track.getInsertSQL());
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Fügt eine Liste an Tracks der Datenbank hinzu.
     *
     * @param tracks Die Liste an Tracks
     */
    static void addTracks(ArrayList<TrackWrapper> tracks) {
        if (tracks.isEmpty())
            return;

        Statement statement;
        try {
            statement = connection.createStatement();
            connection.setAutoCommit(false);

            for (TrackWrapper track : tracks) {
                statement.executeUpdate(track.getInsertSQL());
            }

            connection.commit();
            connection.setAutoCommit(true);

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Entfernt einen Track aus der Datenbank
     *
     * @param track Der zu entfernende Track
     */
    static void removeTrack(TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(track.getDeleteSql());
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Gibt eine Liste aller Playlists zurück, die wiederum nur die IDs der Tracks enthalten. Für das Serialisieren mit
     * JSON zusätzlich zur Liste aller vollständigen Tracks gedacht.
     *
     * @return Liste aller Playlists
     */
    public static List<Playlist> getAllPlaylistsForJSON() {
        try {
            ArrayList<Playlist> res = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM playlists");

            while (resultSet.next()) {
                int id = resultSet.getInt(PlayList.ID);
                String name = resultSet.getString(PlayList.NAME);

                res.add(new Playlist(id, name, getTrackIDsOfPlaylist(id)));
            }

            resultSet.close();
            statement.close();
            return res;
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Gibt eine Liste aller Track-IDs der Playlist zur ID zurück.
     *
     * @param id Die ID der Playlist
     * @return Liste der Track-IDs
     */
    private static List<Integer> getTrackIDsOfPlaylist(int id) {
        try {
            ArrayList<Integer> res = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT track_id FROM playlist_tracks WHERE id = " + id);

            while (resultSet.next()) {
                res.add(resultSet.getInt(PlayList.PLAYLIST_TRACKS_TRACK_ID));
            }

            resultSet.close();
            statement.close();
            return res;
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }

    public static ArrayList<PlayList> getAllPlaylists(ArrayList<TrackWrapper> allTracks) {
        Map<Integer, PlayList> playListMap = new HashMap<>();

        try {
            //alle Playlists laden
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM playlists");

            while (resultSet.next()) {
                int id = resultSet.getInt(PlayList.ID);
                String name = resultSet.getString(PlayList.NAME);

                playListMap.put(id, new PlayList(id, name));
            }

            resultSet.close();
            statement.close();
            //TODO checken
            //Titel den Playlists zuweisen
            statement = connection.createStatement();
            resultSet = statement.executeQuery(String.format("SELECT %s,%s,%s,%s,%s,%s,%s FROM playlist_tracks " +
                            "INNER JOIN music ON music.id = playlist_tracks.track_id",
                    TrackWrapper.ID,
                    TrackWrapper.ALBUM,
                    TrackWrapper.TITLE,
                    TrackWrapper.ARTIST,
                    TrackWrapper.FILE_PATH,
                    TrackWrapper.TRACK_LENGTH,
                    PlayList.PLAYLIST_TRACKS_PLAYLIST_ID));

            while (resultSet.next()) {
                TrackWrapper trackWrapper = readTrack(resultSet);
                Integer playlist_id = resultSet.getInt(PlayList.PLAYLIST_TRACKS_PLAYLIST_ID);

                playListMap.get(playlist_id).addTrack(trackWrapper);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }


        return new ArrayList<>(playListMap.values());
    }
}
