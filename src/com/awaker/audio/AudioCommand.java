package com.awaker.audio;

import com.awaker.global.router.Command;

public enum AudioCommand implements Command {
    //direkt playerbezogene Sachen
    /**
     * Startet die Wiedergabe. Benötigt keine Parameter.
     */
    PLAY("play"),
    /**
     * Spielt den Titel mit der <code>trackId</code> ab.
     */
    PLAY_ID("playId"),
    /**
     * Spielt eine Liste von Titel mit den Ids aus <code>idList</code> ab. Startet mit dem Song mit <code>trackId</code>.
     */
    PLAY_ID_LIST("playIdList"),
    /**
     * Setzt die Position der aktuellen Wiedergabe auf <code>position</code> in Sekunden.
     */
    PLAY_FROM_POSITION("playFromPosition"),
    /**
     * Pausiert die Wiedergabe. Keine Parameter.
     */
    PAUSE("pause"),
    /**
     * Stoppt die Wiedergabe. Keine Parameter.
     */
    STOP("stop"),
    /**
     * Wechselt zwischen Play/Pause. Keine Parameter.
     */
    TOGGLE_PLAY_PAUSE("togglePlayPause"),
    /**
     * Spielt den nächsten Titel ab. Keine Parameter.
     */
    PLAY_NEXT("playNext"),
    /**
     * Spielt den vorherigen Titel ab. Keine Parameter.
     */
    PLAY_PREVIOUS("playPrevious"),
    /**
     * Setzt den Shuffle-Modus auf <code>shuffle</code>.
     */
    SET_SHUFFLE("setShuffle"),
    /**
     * Setzt den Wiederholungsmodus auf <code>repeatMode</code>
     */
    SET_REPEAT_MODE("setRepeatMode"),
    /**
     * Setzt die Lautstärke auf <code>volume</code>
     */
    SET_VOLUME("setVolume"),

    /**
     * Spielt die Playlist mit der <code>playlistId</code>
     */
    PLAY_PLAYLIST("playPlaylist"),
    /**
     * Spielt die Playlist mit <code>playlistId</code> und startet bei dem Track mit <code>trackId</code>
     */
    PLAY_TRACK_OF_PLAYLIST("playTrackOfPlaylist"),
    /**
     * Fügt die Tracks mit den Ids aus <code>idList</code> zur Queue hinzu.
     */
    ADD_TRACKS_TO_QUEUE("addTracksToQueue"),
    /**
     * Entfernt die Tracks mit den Ids aus <code>idList</code> aus der Queue.
     */
    REMOVE_TRACKS_FROM_QUEUE("removeTracksFromQueue"),
    /**
     * Spielt den Track mit <code>trackId</code> aus der Queue.
     */
    PLAY_TRACK_OF_QUEUE("playTrackOfQueue"),
    /**
     * Spielt den Track mit <code>trackId</code> als nächstes.
     */
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
