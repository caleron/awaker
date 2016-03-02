package com.awaker.audio;

import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class PlayList {
    public static final PlayList ALL_TRACKS = new PlayList("Alle", null);

    private Random rand = new Random();
    private String name;

    /**
     * Wenn tracks == null, dann umfasst die Playlist alle Tracks
     */
    private List<TrackWrapper> tracks;

    private TrackWrapper currentTrack;
    private boolean shuffle = true;
    private RepeatMode repeatMode = RepeatMode.REPEAT_MODE_ALL;

    private Stack<TrackWrapper> trackHistory = new Stack<>();

    /**
     * Erstellt eine neue Playlist
     *
     * @param name   Name der Playlist
     * @param tracks Liste der Tracks
     */
    public PlayList(String name, ArrayList<TrackWrapper> tracks) {
        this.name = name;
        this.tracks = tracks;
    }

    /**
     * Wählt den nächsten Track aus
     *
     * @return der nächste Track
     */
    public TrackWrapper nextTrack() {
        if (tracks == null) {
            tracks = MediaManager.getAllTracks();
        }

        //index des aktuellen Tracks bestimmen
        int currentTrackIndex;
        if (currentTrack == null) {
            currentTrackIndex = -1;
        } else {
            currentTrackIndex = tracks.indexOf(currentTrack);
        }

        if (repeatMode == RepeatMode.REPEAT_MODE_FILE && currentTrackIndex >= 0) {
            //aktuellen Track wiederholen
            return currentTrack;
        } else if (currentTrackIndex == -1 || shuffle) {
            //Zufälligen Track auswählen
            int nextIndex = Math.round(rand.nextFloat() * tracks.size());
            currentTrack = tracks.get(nextIndex);

            trackHistory.add(currentTrack);
        } else {
            //nächsten oder ersten Track auswählen
            if (currentTrackIndex + 1 < tracks.size()) {
                currentTrack = tracks.get(currentTrackIndex + 1);
            } else {
                currentTrack = tracks.get(0);
            }
            trackHistory.add(currentTrack);
        }
        return currentTrack;
    }

    /**
     * Gibt den vorherigen Track wieder
     *
     * @return Der vorherige Track
     */
    public TrackWrapper previousTrack() {
        if (currentTrack == null || trackHistory.isEmpty()) {
            return nextTrack();
        } else {
            currentTrack = trackHistory.pop();
            return currentTrack;
        }
    }

    public TrackWrapper getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(TrackWrapper currentTrack) {
        this.currentTrack = currentTrack;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }
}
