package com.awaker.server.json;

public class Answer {
    public static final String TYPE_FILE_STATUS = "file_status";
    public static final String TYPE_STATUS = "status";
    public static final String TYPE_LIBRARY = "library";

    public String type;

    public String colorMode;
    public int currentColor;
    public int whiteBrightness;
    public int animationBrightness;

    public String currentTitle;
    public String currentAlbum;
    public String currentArtist;
    public int repeatMode;
    public int volume;
    public int trackLength;
    public int playPosition;
    public boolean playing;
    public boolean shuffle;

    private boolean fileNotFound;

    public Answer() {
    }

    private Answer(String type) {
        this.type = type;
    }

    static Answer status() {
        return new Answer(TYPE_STATUS);
    }

    static Answer fileNotFound() {
        Answer answer = new Answer(TYPE_FILE_STATUS);
        answer.fileNotFound = true;
        return answer;
    }
}
