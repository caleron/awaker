package com.awaker;

import com.awaker.analyzer.ColorTranslator;
import com.awaker.analyzer.ResultListener;
import com.awaker.audio.PlaybackListener;
import com.awaker.audio.PlayerMaster;
import com.awaker.audio.RepeatMode;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.light.LightController;
import com.awaker.server.Server;
import com.awaker.server.ServerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker implements ResultListener, ServerListener, PlaybackListener {

    private JFrame stringOutputFrame = null;
    private JTextArea stringOutputBox = null;
    private List<Map.Entry<Double, Double>> list;

    private PlayerMaster playerMaster;

    private AwakerPanel panel = null;

    private LightController lightController = null;

    public static boolean isMSWindows = true;

    private Awaker(boolean isWindows) {
        isMSWindows = isWindows;

        new Server(this);

        if (isMSWindows)
            new Timer(1000, e -> playerMaster.printPosition()).start();


        DbManager.init();
        MediaManager.startScanFiles();

        playerMaster = new PlayerMaster(this, this);

        if (isWindows) {
            panel = new AwakerPanel();
        } else {
            lightController = new LightController();
        }
    }


    public static void main(String[] args) {
        String OS = System.getProperty("os.name").toLowerCase();

        if (OS.contains("win")) {
            Awaker awaker = new Awaker(true);

            JFrame frame = new JFrame("Awaker");
            frame.setContentPane(awaker.panel);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(800, 500);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } else {
            new Awaker(false);
        }
    }


    @Override
    public void newResults(List<Map.Entry<Double, Double>> list) {
        this.list = list;

        if (panel != null) {
            SwingUtilities.invokeLater(panel::repaint);
        } else if (lightController != null) {
            lightController.updateColor(ColorTranslator.translatePartition2(list));
        }
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
    public void setColorBrightness(int brightness) {

    }

    @Override
    public void setColor(Color color) {
        if (lightController != null) {
            lightController.updateColor(color);
        }
    }

    @Override
    public void setColorMode(boolean custom) {

    }

    @Override
    public void stringReceived(String str) {
        if (isMSWindows) {
            if (stringOutputFrame == null) {
                stringOutputFrame = new JFrame("Ausgabe");
                stringOutputFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

                stringOutputBox = new JTextArea(30, 50);
                JScrollPane scrollPane = new JScrollPane(stringOutputBox);
                stringOutputFrame.setContentPane(scrollPane);
                stringOutputFrame.pack();
            }
            stringOutputFrame.setVisible(true);
            stringOutputBox.setText(stringOutputBox.getText() + "\n" + str);
        }
    }

    @Override
    public void setWhiteBrightness(int brightness) {
        //wert zwischen 0 und 100 sicherstellen
        brightness = Math.max(0, Math.min(100, brightness));

        lightController.setWhiteBrightness(brightness);
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

    @Override
    public void playbackPaused() {
        if (lightController != null) {
            lightController.fadeOutColorLights();
        }
    }

    private class AwakerPanel extends JPanel {

        AwakerPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        //linke Maustaste
                        playerMaster.tooglePlayPause();
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        //rechte Maustaste
                        playerMaster.playNext();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            final int FREQ_AREA = 5000;
            final int MAX_AMP = 5000;

            int width = getWidth();
            int yBottom = getHeight() - 10;


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
            Color color = ColorTranslator.translatePartition2(list);
            int fieldWidth = width / 4;
            int fieldHeight = 100;//getHeight() / 3;
            int space = width / 16;

            g.setColor(new Color(color.getRed(), 0, 0));
            g.fillRect(space, 0, fieldWidth, fieldHeight);

            g.setColor(new Color(0, color.getGreen(), 0));
            g.fillRect(space * 2 + fieldWidth, 0, fieldWidth, fieldHeight);

            g.setColor(new Color(0, 0, color.getBlue()));
            g.fillRect(space * 3 + fieldWidth * 2, 0, fieldWidth, fieldHeight);
        }
    }
}
