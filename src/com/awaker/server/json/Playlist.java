package com.awaker.server.json;

import java.util.ArrayList;
import java.util.List;

public class Playlist {

    public int id;
    public String name;
    public List<Integer> trackIdList = new ArrayList<>();

    public Playlist(int id, String name, List<Integer> trackIdList) {
        this.id = id;
        this.name = name;
        this.trackIdList = trackIdList;
    }
}
