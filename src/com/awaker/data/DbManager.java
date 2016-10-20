package com.awaker.data;

import com.awaker.audio.PlayList;
import com.awaker.config.Config;
import com.awaker.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
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
            statement.executeUpdate(Config.getCreateTableSQL());
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
     * @throws SQLException Bei Fehlern
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
            PreparedStatement statement = connection.prepareStatement(TrackWrapper.getInsertSQL());

            statement.setString(1, track.artist);
            statement.setString(2, track.title);
            statement.setString(3, track.album);
            statement.setString(4, track.filePath);
            statement.setInt(5, track.trackLength);
            statement.executeUpdate();
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

        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(TrackWrapper.getInsertSQL());
            connection.setAutoCommit(false);

            for (TrackWrapper track : tracks) {
                statement.setString(1, track.artist);
                statement.setString(2, track.title);
                statement.setString(3, track.album);
                statement.setString(4, track.filePath);
                statement.setInt(5, track.trackLength);
                statement.executeUpdate();
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
     * Gibt alle Playlists mit korrekten Referenzen aus.
     *
     * @param allTracks Alle Tracks.
     * @return Eine Liste aller Playlists.
     */
    static ArrayList<PlayList> getAllPlaylists(ArrayList<TrackWrapper> allTracks) {
        Map<Integer, PlayList> playListMap = new HashMap<>();

        try {
            //alle Playlists laden
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + PlayList.TABLE_NAME);

            while (resultSet.next()) {
                int id = resultSet.getInt(PlayList.ID);
                String name = resultSet.getString(PlayList.NAME);

                playListMap.put(id, new PlayList(id, name));
            }

            resultSet.close();
            statement.close();

            //Titel den Playlists zuweisen
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM " + PlayList.PLAYLIST_TRACKS_TABLE_NAME);

            while (resultSet.next()) {
                Integer trackId = resultSet.getInt(PlayList.PLAYLIST_TRACKS_TRACK_ID);
                Integer playlistId = resultSet.getInt(PlayList.PLAYLIST_TRACKS_PLAYLIST_ID);

                for (TrackWrapper track : allTracks) {
                    if (track.getId() == trackId) {
                        playListMap.get(playlistId).addTrack(track);
                        break;
                    }
                }
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }

        return new ArrayList<>(playListMap.values());
    }

    /**
     * Erstellt eine neue Playlists.
     *
     * @param name Der name der Playlist.
     */
    static void createPlaylist(String name) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(new PlayList(name).getInsertSQL());

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Löscht eine Playlist.
     *
     * @param playListId Die Id der zu löschenden Playlist.
     */
    static void removePlaylist(int playListId) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(String.format("DELETE FROM playlists WHERE id = %d", playListId));
            statement.executeUpdate(String.format("DELETE FROM playlist_tracks WHERE playlist_id = %d", playListId));
            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Fügt einen Track zu einer Playlist hinzu.
     *
     * @param playList Die Playlist.
     * @param track    Der Track.
     */
    static void addTrackToPlaylist(PlayList playList, TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(String.format("INSERT INTO playlist_tracks (playlist_id, track_id) VALUES (%d, %d)"
                    , playList.getId(), track.getId()));

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Entfernt einen Track von einer Playlist.
     *
     * @param playList Die Playlist.
     * @param track    Der zu entfernende Track.
     */
    static void removeTrackFromPlaylist(PlayList playList, TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(String.format("DELETE FROM playlist_tracks WHERE playlist_id = %d AND track_id = %d"
                    , playList.getId(), track.getId()));

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Gibt eine HashMap der Konfiguration zurück.
     *
     * @return HashMap mit Schlüssel-Wert-Paaren
     */
    public static HashMap<String, String> getConfig() {
        try {
            HashMap<String, String> res = new HashMap<>();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM config");

            while (resultSet.next()) {
                res.put(resultSet.getString("name"), resultSet.getString("value"));
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
     * Setzt eine Einstellung.
     *
     * @param key   Der Schlüssel.
     * @param value Der neue Wert.
     */
    public static void setConfig(String key, String value) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(String.format("REPLACE INTO config (name, value) VALUES (\"%s\", \"%s\")", key, value));

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }
}
