package com.awaker.server;

import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Answer;

import java.awt.*;
import java.io.InputStream;

public interface ServerListener {
    TrackWrapper downloadFile(InputStream is, int length, String fileName, boolean play);

    boolean containsFile(TrackWrapper track);

    void play();

    void playFromPosition(int position);

    void pause();

    void stop();

    void playNext();

    void playPrevious();

    void setShuffle(boolean shuffle);

    /**
     * Setzt den Wiederholungsmodus. Keine Wiederholung 0, Datei wiederholen 1, Alles wiederholen 2
     *
     * @param repeatMode Der Modus
     */
    void setRepeatMode(String repeatMode);

    void setVolume(int volume);

    void setAnimationBrightness(int brightness, boolean smooth);

    void setColor(Color color, boolean smooth);

    void setWhiteBrightness(int brightness, boolean smooth);

    void changeVisualisation(String newType);

    Answer getStatus(Answer answer);

    void togglePlayPause();

    /**
     * Setzt den Farbmodus. Mögliche Werte sind: music, custom, colorCircle
     *
     * @param mode der Modus
     */
    void setColorMode(String mode);

    void playPlaylist(int id);

    void playTrackOfPlaylist(int playlistId, int trackId);

    void createPlaylist(String name);

    void removePlaylist(int id);

    void addTracksToPlaylist(int playlistId, Integer[] list);

    void removeTracksFromPlaylist(int playlistId, Integer[] list);

    void stringReceived(String str);

    void shutdown();

    void play(int trackId);

    void playIdList(Integer playNowId, Integer[] list);

    void playTrackNext(int id);

    void addTracksToQueue(Integer[] list);

    void removeTracksFromQueue(Integer[] idList);

    void playTrackOfQueue(Integer id);
}
