package com.awaker.server.json;

import com.awaker.Awaker;
import com.awaker.audio.PlayerMaster;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.gpio.LightController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Answer {
    private static final String TYPE_FILE_STATUS = "file_status";
    private static final String TYPE_STATUS = "status";
    private static final String TYPE_LIBRARY = "library";
    private static final String TYPE_CONFIG = "config";
    private static final String TYPE_ACTION = "action";

    private String type;

    public String colorMode;
    public Integer currentColor;
    public Integer whiteBrightness;
    public Integer animationBrightness;

    public String currentTitle;
    public String currentAlbum;
    public String currentArtist;
    public Integer currentTrackId;
    public String repeatMode;
    public Integer volume;
    public Integer trackLength;
    public Integer playPosition;
    public Boolean playing;
    public Boolean shuffle;

    public List<Track> tracks;
    public List<Playlist> playLists;
    public Playlist trackQueue;

    public String name;
    public String value;
    public HashMap<String, String> config;
    public String[] configOptions;

    private Boolean fileNotFound;

    private Answer(String type) {
        this.type = type;
    }

    public static Answer status() {
        Answer answer = new Answer(TYPE_STATUS);
        PlayerMaster.getInstance().writeStatus(answer);
        if (!Awaker.isMSWindows) {
            LightController.getInstance().writeStatus(answer);
        }
        return answer;
    }

    public static Answer library() {
        Answer answer = new Answer(TYPE_LIBRARY);
        answer.tracks = new ArrayList<>();
        ArrayList<TrackWrapper> allTracks = MediaManager.getAllTracks();

        answer.tracks.addAll(allTracks.stream()
                .map(track -> new Track(track.getId(), track.title, track.artist, track.album, track.trackLength))
                .collect(Collectors.toList()));

        answer.playLists = MediaManager.getPlayListsForJson();
        return answer;
    }

    public static Answer config() {
        return new Answer(TYPE_CONFIG);
    }

    public static Answer action() {
        return new Answer(TYPE_ACTION);
    }

    public static Answer fileStatus(boolean fileNotFound) {
        Answer answer = new Answer(TYPE_FILE_STATUS);
        answer.fileNotFound = fileNotFound;
        return answer;
    }
}
