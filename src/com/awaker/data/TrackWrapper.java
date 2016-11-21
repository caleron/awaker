package com.awaker.data;

import java.util.Date;

public class TrackWrapper {
    private static final String TABLE_NAME = "music";

    public static final String ID = "id";
    public static final String ALBUM = "album";
    public static final String ARTIST = "artist";
    public static final String TITLE = "title";
    public static final String FILE_PATH = "file";
    public static final String ADD_DATE = "add_date"; //timestamp mit millisekunden
    public static final String TRACK_LENGTH = "length";

    private int id;
    public final String title;
    public final String artist;
    public String album;
    public String filePath;
    public Date addDate;
    public int trackLength; //in Sekunden

    TrackWrapper(int id, String title, String artist, String album, String filePath, Date addDate, int trackLength) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.addDate = addDate;
        this.trackLength = trackLength;
    }

    static String getInsertSQL() {
        return String.format("INSERT INTO %s (%s,%s,%s,%s, %s, %s) VALUES (?,?,?,?,?,?)", TABLE_NAME,
                ARTIST, TITLE, ALBUM, FILE_PATH, ADD_DATE, TRACK_LENGTH);
    }

    String getDeleteSql() {
        return String.format("DELETE FROM %s WHERE %s = %s", TABLE_NAME, ID, id);
    }

    static String getCreateTableSql() {
        return String.format("CREATE TABLE IF NOT EXISTS \"%s\" " +
                "(\"%s\" INTEGER PRIMARY KEY, " +
                "\"%s\" TEXT," +
                "\"%s\" TEXT," +
                "\"%s\" TEXT," +
                "\"%s\" TEXT," +
                "\"%s\" INTEGER,"
                + "\"%s\" INTEGER)", TABLE_NAME, ID, TITLE, ARTIST, ALBUM, FILE_PATH, ADD_DATE, TRACK_LENGTH);
    }

    public int getId() {
        return id;
    }
}
