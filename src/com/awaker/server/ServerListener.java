package com.awaker.server;

import com.awaker.data.TrackWrapper;
import com.awaker.server.json.Answer;

import java.awt.*;
import java.io.InputStream;

public interface ServerListener {
    void downloadFile(InputStream is, int length, String fileName, boolean play);

    boolean playFile(TrackWrapper track);

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
    void setRepeatMode(int repeatMode);

    void setVolume(int volume);

    void setColorBrightness(int brightness);

    void setColor(Color color);

    void setWhiteBrightness(int brightness);

    void changeVisualisation(String newType);

    Answer getStatus(Answer answer);

    void togglePlayPause();

    /**
     * Setzt den Farbmodus. Mögliche Werte sind: music, custom, colorCircle
     *
     * @param mode der Modus
     */
    void setColorMode(String mode);

    void stringReceived(String str);

    void shutdown();
}
