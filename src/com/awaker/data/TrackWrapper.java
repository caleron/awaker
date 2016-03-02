package com.awaker.data;

public class TrackWrapper {
    public static final String TABLE_NAME = "music";

    public static final String ID = "id";
    public static final String ALBUM = "album";
    public static final String ARTIST = "artist";
    public static final String TITLE = "title";
    public static final String FILE_PATH = "file";

    public int id;
    public String title;
    public String artist;
    public String album;
    public String filePath;

    public TrackWrapper(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public TrackWrapper(int id, String title, String artist, String album, String filePath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
    }

    public String getInsertSQL() {
        return String.format("INSERT INTO %s (%s,%s,%s,%s) VALUES (\"%s\",\"%s\",\"%s\",\"%s\")", TABLE_NAME,
                ARTIST, TITLE, ALBUM, FILE_PATH,
                artist, title, album, filePath);
    }

    public String getDeleteSql() {
        return String.format("DELETE FROM %s WHERE %s = %s", TABLE_NAME, ID, id);
    }
}
