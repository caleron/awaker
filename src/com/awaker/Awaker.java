package com.awaker;

import com.awaker.analyzer.ResultListener;
import com.awaker.audio.PlayerMaster;
import com.awaker.audio.RepeatMode;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.server.Server;
import com.awaker.server.ServerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker extends JPanel implements ResultListener, ServerListener {

    List<Map.Entry<Double, Double>> list;

    PlayerMaster playerMaster;

    public Awaker() {
        new Server(this);

        DbManager.init();
        MediaManager.startScanFiles();

        playerMaster = new PlayerMaster(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                playerMaster.tooglePlayPause();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        final int FREQ_AREA = 5000;
        final int MAX_AMP = 5000;

        int width = getWidth();
        int yBottom = getHeight() - 10;

        //g.setColor(ColorTranslator.translateDurchschnitt(list));

        g.setColor(Color.GRAY);
        g.fillRect(0, 0, width, getHeight());
        ((Graphics2D) g).setStroke(new BasicStroke(3));

        if (list != null && !list.isEmpty()) {
            g.setColor(Color.BLACK);

            for (Map.Entry<Double, Double> entry : list) {
                int x = (int) ((entry.getKey() / FREQ_AREA) * width);
                int y = (int) (yBottom - ((entry.getValue() / MAX_AMP) * yBottom));
                g.drawLine(x, yBottom, x, y);
            }

        }
    }

    public static void main(String[] args) {
        Awaker awaker = new Awaker();

        JFrame frame = new JFrame("Awaker");
        frame.setContentPane(awaker);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    @Override
    public void newResults(List<Map.Entry<Double, Double>> list) {
        this.list = list;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public boolean playFile(TrackWrapper track) {
        return playerMaster.playFile(track);
    }

    @Override
    public void downloadFile(InputStream is, int length, String fileName, boolean play) {
        TrackWrapper track = MediaManager.downloadFile(is, length, fileName);
        if (play && track != null) {
            playFile(track);
        }
    }

    @Override
    public void play() {
        playerMaster.play();
    }

    @Override
    public void playFromPosition(int position) {
        playerMaster.playFromPosition(position);
    }

    @Override
    public void pause() {
        playerMaster.pause();
    }

    @Override
    public void stop() {
        playerMaster.stop();
    }

    @Override
    public void playNext() {
        playerMaster.playNext();
    }

    @Override
    public void playPrevious() {
        playerMaster.playPrevious();
    }

    @Override
    public void setShuffle(boolean shuffle) {
        playerMaster.setShuffle(shuffle);
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        RepeatMode mode;
        switch (repeatMode) {
            case 0:
                mode = RepeatMode.REPEAT_MODE_NONE;
                break;
            case 1:
                mode = RepeatMode.REPEAT_MODE_FILE;
                break;
            default:
                mode = RepeatMode.REPEAT_MODE_ALL;
                break;
        }

        playerMaster.setRepeatMode(mode);
    }

    @Override
    public void setBrightness(int brightness) {

    }

    @Override
    public void changeVisualisation(String newType) {

    }

    @Override
    public String getStatus() {
        return playerMaster.getStatus();
    }

    @Override
    public void togglePlayPause() {
        playerMaster.tooglePlayPause();
    }
}
