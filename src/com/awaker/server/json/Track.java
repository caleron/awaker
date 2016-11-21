package com.awaker.server.json;

import com.awaker.data.TrackWrapper;

import java.util.Date;

public class Track {

    public int id;
    public String title;
    public String artist;
    public String album;
    public long addDate; //in ms
    public int trackLength; //in Sekunden

    public Track(TrackWrapper track) {
        this.id = track.getId();
        this.title = track.title;
        this.artist = track.artist;
        this.album = track.album;
        this.addDate = track.addDate.getTime();
        this.trackLength = track.trackLength;
    }

    public Track(int id, String title, String artist, String album, Date addDate, int trackLength) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.addDate = addDate.getTime();
        this.trackLength = trackLength;
    }
}
