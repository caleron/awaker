package com.awaker.audio;

import com.awaker.global.Command;

public enum AudioCommand implements Command {
    //direkt playerbezogene Sachen
    PLAY("play"),
    PLAY_ID("playId"),
    PLAY_ID_LIST("playIdList"),
    PLAY_FROM_POSITION("playFromPosition"),
    PAUSE("pause"),
    STOP("stop"),
    TOGGLE_PLAY_PAUSE("togglePlayPause"),
    PLAY_NEXT("playNext"),
    PLAY_PREVIOUS("playPrevious"),
    SET_SHUFFLE("setShuffle"),
    SET_REPEAT_MODE("setRepeatMode"),
    SET_VOLUME("setVolume"),

    PLAY_PLAYLIST("playPlaylist"),
    PLAY_TRACK_OF_PLAYLIST("playTrackOfPlaylist"),
    ADD_TRACKS_TO_QUEUE("addTracksToQueue"),
    REMOVE_TRACKS_FROM_QUEUE("removeTracksFromQueue"),
    PLAY_TRACK_OF_QUEUE("playTrackOfQueue"),
    PLAY_TRACK_NEXT("playTrackNext");

    private final String action;

    AudioCommand(String action) {
        this.action = action;
    }

    @Override
    public String getAction() {
        return action;
    }
}
