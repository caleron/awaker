package com.awaker.server;

public interface ServerListener {
    boolean playFile(String name);

    void play();

    void playFromPosition(int position);

    void pause();

    void stop();

    void setBrightness(int brightness);

    void changeVisualisation(String newType);

    String getStatus();
}
