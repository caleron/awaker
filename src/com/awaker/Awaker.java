package com.awaker;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.ColorTranslator;
import com.awaker.audio.PlaybackListener;
import com.awaker.audio.PlayerMaster;
import com.awaker.audio.RepeatMode;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.gpio.AnalogControls;
import com.awaker.gpio.AnalogListener;
import com.awaker.gpio.LightController;
import com.awaker.server.MyServer;
import com.awaker.server.UploadServer;
import com.awaker.server.ServerListener;
import com.awaker.server.json.Answer;
import com.awaker.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker implements AnalyzeResultListener, ServerListener, PlaybackListener, AnalogListener {
    //Ausgabefenster und -feld beim Betrieb auf Windows
    private JFrame stringOutputFrame = null;
    private JTextArea stringOutputBox = null;
    private AwakerPanel panel = null;

    private PlayerMaster playerMaster;

    private MyServer server;

    private LightController lightController = null;

    private AnalogControls analogControls = null;

    public static boolean isMSWindows = true;

    private Awaker(boolean isWindows) {
        isMSWindows = isWindows;

        new UploadServer(this);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Log.error(e));

        /*if (isMSWindows)
            new Timer(1000, e -> playerMaster.printPosition()).start();*/

        DbManager.init();
        MediaManager.startScanFiles();

        server = new MyServer(this);
        server.start();

        playerMaster = new PlayerMaster(this, this);

        if (isWindows) {
            panel = new AwakerPanel();
        } else {
            lightController = new LightController();
            //TODO auskommentieren, wenn adc angeschlossen
            //analogControls = new AnalogControls(this);
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
        if (isMSWindows) {
            panel.fftResultList = list;
            SwingUtilities.invokeLater(panel::repaint);
        } else {
            lightController.updateColor(ColorTranslator.translatePartition2(list), true);
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
    public boolean containsFile(TrackWrapper track) {
        track = DbManager.getTrack(track.title, track.artist);
        if (track != null) {
            File file = new File(track.filePath);

            return file.exists();
        } else {
            return false;
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
    public void setVolume(int volume) {
        playerMaster.setVolume(volume);
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
    public void setAnimationBrightness(int brightness) {
        if (!isMSWindows) {
            lightController.setAnimationBrightness(brightness);
        }
    }

    @Override
    public void setColor(Color color) {
        if (!isMSWindows) {
            lightController.updateColor(color, false);
        }
    }

    @Override
    public void setColorMode(String mode) {
        if (!isMSWindows) {
            lightController.setColorMode(mode);
        }
        playerMaster.setColorMode(!mode.equals("music"));
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
    public void shutdown() {
        playerMaster.stop();

        System.exit(0);
    }

    @Override
    public void setWhiteBrightness(int brightness) {
        if (!isMSWindows) {
            lightController.setWhiteBrightness(brightness);
        }
    }

    @Override
    public void setRed(int brightness) {
        if (!isMSWindows) {
            lightController.setRedBrightness(brightness);
        }
    }

    @Override
    public void setGreen(int brightness) {
        if (!isMSWindows) {
            lightController.setGreenBrightness(brightness);
        }
    }

    @Override
    public void setBlue(int brightness) {
        if (!isMSWindows) {
            lightController.setBlueBrightness(brightness);
        }
    }

    @Override
    public void changeVisualisation(String newType) {

    }

    @Override
    public void playPlaylist(int id) {
        playerMaster.playPlaylist(MediaManager.getPlayList(id));
    }

    @Override
    public void playTrackOfPlaylist(int playlistId, int trackId) {
        playerMaster.playTrackOfPlaylist(MediaManager.getPlayList(playlistId), MediaManager.getTrack(trackId));
    }

    @Override
    public void createPlaylist(String name) {
        MediaManager.createPlaylist(name);
    }

    @Override
    public void removePlaylist(int id) {
        MediaManager.removePlaylist(id);
    }

    @Override
    public void addTrackToPlaylist(int playlistId, int trackId) {
        MediaManager.addTrackToPlaylist(playlistId, trackId);
    }

    @Override
    public void removeTrackFromPlaylist(int playlistId, int trackId) {
        MediaManager.removeTrackFromPlaylist(playlistId, trackId);
    }


    @Override
    public Answer getStatus(Answer answer) {
        playerMaster.getStatus(answer);
        if (!isMSWindows) {
            lightController.getStatus(answer);
        }
        return answer;
    }

    @Override
    public void togglePlayPause() {
        playerMaster.tooglePlayPause();
    }


    @Override
    public void playbackPaused() {
        if (!isMSWindows) {
            lightController.fadeOutColorLights();
        }
    }

    /**
     * Panel zur grafischen Darstellung der Frequenzanalyse unter Windows
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    private class AwakerPanel extends JPanel {
        private static final long serialVersionUID = 1646200901514802932L;

        //Liste mit den Ergebnissen aus der Frequenzanalyse mit FFT
        private List<Map.Entry<Double, Double>> fftResultList;

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

            if (fftResultList != null && !fftResultList.isEmpty()) {
                g.setColor(Color.BLACK);

                for (Map.Entry<Double, Double> entry : fftResultList) {
                    int x = (int) ((entry.getKey() / FREQ_AREA) * width);
                    int y = (int) (yBottom - ((entry.getValue() / MAX_AMP) * yBottom));
                    g.drawLine(x, yBottom, x, y);
                }

            }
            Color color = ColorTranslator.translatePartition2(fftResultList);
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
