package com.awaker.data;

import com.awaker.global.router.Command;

public enum MediaCommand implements Command {
    //playlists und queue
    CREATE_PLAYLIST("createPlaylist"),
    REMOVE_PLAYLIST("removePlaylist"),
    ADD_TRACKS_TO_PLAYLIST("addTracksToPlaylist"),
    REMOVE_TRACKS_FROM_PLAYLIST("removeTracksFromPlaylist"),
    CHECK_FILE("checkFile");

    private String action;

    MediaCommand(String action) {
        this.action = action;
    }

    @Override
    public String getAction() {
        return action;
    }
}
