package com.awaker.server.json;

public class Track {

    public int id;
    public String title;
    public String artist;
    public String album;
    public int trackLength; //in Sekunden

    public Track(int id, String title, String artist, String album, int trackLength) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.trackLength = trackLength;
    }
}
