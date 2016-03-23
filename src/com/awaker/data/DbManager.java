package com.awaker.data;

import java.sql.*;
import java.util.ArrayList;

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
            System.err.println("Fehler beim Laden des JDBC-Treibers");
            e.printStackTrace();
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
                System.out.println("Database Connection established");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        setupDb();
    }

    /**
     * Erstellt die Datenbanktabellen (1)
     */
    private static void setupDb() {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(TrackWrapper.getCreateTableSql());
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gibt alle Tracks zurück
     *
     * @return ArrayList mit allen Tracks
     */
    public static ArrayList<TrackWrapper> getAllTracks() {
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
            e.printStackTrace();
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
            e.printStackTrace();
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
    public static void addTrack(TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(track.getInsertSQL());
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fügt eine Liste an Tracks der Datenbank hinzu.
     *
     * @param tracks Die Liste an Tracks
     */
    public static void addTracks(ArrayList<TrackWrapper> tracks) {
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
            e.printStackTrace();
        }
    }

    /**
     * Entfernt einen Track aus der Datenbank
     *
     * @param track Der zu entfernende Track
     */
    public static void removeTrack(TrackWrapper track) {
        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate(track.getDeleteSql());
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
