package com.awaker.audio;

import com.awaker.data.TrackWrapper;

import java.util.ArrayList;

public class PlayList {
    public static final PlayList ALL_TRACKS = new PlayList("Alle", null);

    String name;
    ArrayList<TrackWrapper> tracks;

    public PlayList(String name, ArrayList<TrackWrapper> tracks) {
        this.name = name;
        this.tracks = tracks;
    }
}
