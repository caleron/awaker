package com.awaker.server.json;

import java.util.List;

public class Answer {
    private static final String TYPE_FILE_STATUS = "file_status";
    private static final String TYPE_STATUS = "status";
    private static final String TYPE_LIBRARY = "library";

    private String type;

    public String colorMode;
    public int currentColor;
    public int whiteBrightness;
    public int animationBrightness;

    public String currentTitle;
    public String currentAlbum;
    public String currentArtist;
    public int currentTrackId;
    public int repeatMode;
    public int volume;
    public int trackLength;
    public int playPosition;
    public boolean playing;
    public boolean shuffle;

    public List<Track> tracks;
    public List<Playlist> playLists;

    private boolean fileNotFound;

    public Answer() {
    }

    private Answer(String type) {
        this.type = type;
    }

    public static Answer status() {
        return new Answer(TYPE_STATUS);
    }

    public static Answer library() {
        return new Answer(TYPE_LIBRARY);
    }

    public static Answer fileNotFound() {
        Answer answer = new Answer(TYPE_FILE_STATUS);
        answer.fileNotFound = true;
        return answer;
    }
}
