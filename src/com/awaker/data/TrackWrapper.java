package com.awaker.data;

public class TrackWrapper {
    public static final String TABLE_NAME = "music";

    public static final String ID = "id";
    public static final String ALBUM = "album";
    public static final String ARTIST = "artist";
    public static final String TITLE = "title";
    public static final String FILE_PATH = "file";
    public static final String TRACK_LENGTH = "length";

    public int id;
    public String title;
    public String artist;
    public String album;
    public String filePath;
    public int trackLength; //in Sekunden

    public TrackWrapper(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    TrackWrapper(int id, String title, String artist, String album, String filePath, int trackLength) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.trackLength = trackLength;
    }

    String getInsertSQL() {
        return String.format("INSERT INTO %s (%s,%s,%s,%s, %s) VALUES (\"%s\",\"%s\",\"%s\",\"%s\", \"%s\")", TABLE_NAME,
                ARTIST, TITLE, ALBUM, FILE_PATH, TRACK_LENGTH,
                artist, title, album, filePath, trackLength);
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
                "\"%s\" TEXT,"
                + "\"length\" INTEGER)", TABLE_NAME, ID, TITLE,ARTIST,ALBUM, FILE_PATH);
    }
}
