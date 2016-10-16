package com.awaker;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.analyzer.ColorTranslator;
import com.awaker.audio.AudioCommand;
import com.awaker.audio.PlayerMaster;
import com.awaker.automation.Automator;
import com.awaker.config.Config;
import com.awaker.control.RaspiControl;
import com.awaker.data.DbManager;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import com.awaker.global.*;
import com.awaker.gpio.AnalogControls;
import com.awaker.gpio.LightController;
import com.awaker.server.HttpUploadServer;
import com.awaker.server.MyWebSocketServer;
import com.awaker.server.WebContentServer;
import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;
import com.awaker.server.json.Track;
import com.awaker.util.Log;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Awaker implements AnalyzeResultListener, CommandHandler, EventReceiver {
    //Ausgabefenster und -feld beim Betrieb auf Windows
    private JFrame stringOutputFrame = null;
    private JTextArea stringOutputBox = null;
    private AwakerPanel panel = null;

    private final PlayerMaster playerMaster;

    public static final Gson GSON = new Gson();
    private final MyWebSocketServer server;

    private LightController lightController = null;

    public static boolean isMSWindows = true;

    private Awaker(boolean isWindows) {
        isMSWindows = isWindows;

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Log.error(e));

        /*if (isMSWindows)
            new Timer(1000, e -> playerMaster.printPosition()).start();*/

        DbManager.init();
        Config.init();
        MediaManager.init();
        RaspiControl.init();

        playerMaster = new PlayerMaster(this);

        if (isWindows) {
            panel = new AwakerPanel();
        } else {
            new AnalogControls();
            lightController = new LightController();
        }
        new Automator(lightController);

        server = new MyWebSocketServer();
        server.start();

        WebContentServer.start();
        HttpUploadServer.start();

        EventRouter.registerReceiver(this, GlobalEvent.PLAYBACK_PAUSED);
        EventRouter.registerReceiver(this, GlobalEvent.PLAYBACK_NEW_SONG);
        EventRouter.registerReceiver(this, GlobalEvent.SHUTDOWN);
    }


    public static void main(String[] args) {
        String OS = System.getProperty("os.name").toLowerCase();
        printVersion();

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

    private static void printVersion() {
        try {
            File f = new File(Awaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Log.message("Starte Version vom " + new Date(f.lastModified()).toString());
        } catch (URISyntaxException e) {
            Log.error(e);
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
    public Answer handleCommand(Command command, CommandData data) {
        if (!(command instanceof DataCommand)) {
            throw new RuntimeException("Received Wrong Command");
        }

        DataCommand cmd = (DataCommand) command;

        Answer answer;
        switch (cmd) {
            case GET_LIBRARY:
                answer = Answer.library();
                answer.tracks = new ArrayList<>();
                ArrayList<TrackWrapper> allTracks = MediaManager.getAllTracks();

                answer.tracks.addAll(allTracks.stream()
                        .map(track -> new Track(track.getId(), track.title, track.artist, track.album, track.trackLength))
                        .collect(Collectors.toList()));

                answer.playLists = MediaManager.getPlayListsForJson();
                break;
            case GET_STATUS:
                answer = Answer.status();
                playerMaster.getStatus(answer);
                if (!isMSWindows) {
                    lightController.getStatus(answer);
                }
                break;
            case SEND_STRING:
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
                    stringOutputBox.setText(stringOutputBox.getText() + "\n" + data.text);
                }
                answer = Answer.fileNotFound();
                break;
            default:
                return null;
        }
        return answer;
    }

    @Override
    public void receiveGlobalEvent(GlobalEvent globalEvent) {
        switch (globalEvent) {
            case PLAYBACK_NEW_SONG:
                server.sendStatus();
                break;
            case PLAYBACK_PAUSED:
                if (!isMSWindows) {
                    lightController.fadeOutColorLights();
                }
                break;
        }
    }

    /**
     * Panel zur grafischen Darstellung der Frequenzanalyse unter Windows
     */
    private static class AwakerPanel extends JPanel {
        private static final long serialVersionUID = 1646200901514802932L;

        //Liste mit den Ergebnissen aus der Frequenzanalyse mit FFT
        private List<Map.Entry<Double, Double>> fftResultList;

        AwakerPanel() {
            addMouseListener(new MyMouseAdapter());
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

        private static class MyMouseAdapter extends MouseAdapter {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    //linke Maustaste
                    CommandRouter.handleCommand(AudioCommand.TOGGLE_PLAY_PAUSE);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    //rechte Maustaste
                    CommandRouter.handleCommand(AudioCommand.PLAY_NEXT);
                }
            }
        }
    }
}
