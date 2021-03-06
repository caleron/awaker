package com.awaker.data;

import com.awaker.audio.PlayList;
import com.awaker.config.Config;
import com.awaker.util.Log;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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
            statement.executeUpdate(TrackWrapper.getCreateColorsTableSql());
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
    static TrackWrapper getTrack(String title, String artist) {
        try {
            String sql;
            if (artist == null || artist.length() == 0) {
                sql = "SELECT * FROM music WHERE title LIKE ? LIMIT 1";
            } else {
                sql = "SELECT * FROM music WHERE title LIKE ? AND artist LIKE ? LIMIT 1";
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, title);

            if (artist != null && artist.length() > 0) {
                statement.setString(2, artist);
            }

            ResultSet resultSet = statement.executeQuery();
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
        Date addDate = new Date(resultSet.getLong(TrackWrapper.ADD_DATE));
        int length = resultSet.getInt(TrackWrapper.TRACK_LENGTH);

        return new TrackWrapper(id, title, artist, album, file, addDate, length);
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
            statement.setLong(5, track.addDate.getTime());
            statement.setInt(6, track.trackLength);
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
                statement.setLong(5, track.addDate.getTime());
                statement.setInt(6, track.trackLength);
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

    /**
     * Sets the music color array for the given track id in the database. Stores also a version int for future usage.
     *
     * @param trackId the id of the track to which the colors belong
     * @param colors  the color array, first int should be the sample rate
     * @param version the version of the used analyzer for future usage.
     */
    public static void setMusicColors(int trackId, byte[] colors, int version) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "REPLACE INTO music_colors(music_id, colors, analyzer_version, analyze_time) VALUES (?,?,?,?)");

            statement.setInt(1, trackId);
            statement.setBytes(2, colors);
            statement.setInt(3, version);
            statement.setDate(4, new java.sql.Date(Instant.now().getEpochSecond() * 1000));
            statement.executeUpdate();

            statement.close();
        } catch (SQLException e) {
            Log.error(e);
        }
    }

    /**
     * Selects one track from the database which has not been analyzed yet.
     *
     * @return a {@link TrackWrapper} for a not analyzed track
     */
    public static TrackWrapper getTrackWithoutColors() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT id FROM music WHERE id NOT IN ( SELECT music_id FROM music_colors) LIMIT 1");

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                resultSet.close();
                return MediaManager.getTrack(id);
            } else {
                resultSet.close();
            }
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }

    /**
     * Retrieves the color array from the database for a track.
     *
     * @param trackId the id of the track for which the colors should be retrieved.
     * @return byte array, to be read as int array with the first int as sample rate, or null on error or if colours not
     * found
     */
    public static byte[] getMusicColors(int trackId) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT colors FROM music_colors WHERE music_id = ?");
            statement.setInt(1, trackId);
            ResultSet rs = statement.executeQuery();

            byte[] colors = null;
            if (rs.next()) {
                colors = rs.getBytes("colors");
            }

            rs.close();
            return colors;
        } catch (SQLException e) {
            Log.error(e);
        }
        return null;
    }
}
