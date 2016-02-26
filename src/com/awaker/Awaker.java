package com.awaker;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.analyzer.ResultListener;
import com.awaker.audio.CustomPlayer;
import com.awaker.audio.PlayerListener;
import com.awaker.server.Server;
import com.awaker.server.ServerListener;
import javazoom.jl.decoder.JavaLayerException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker extends JPanel implements ResultListener, PlayerListener, ServerListener {

    List<Map.Entry<Double, Double>> list;

    FFTAnalyzer analyzer = new FFTAnalyzer(this);

    Timer timer;

    CustomPlayer player;

    public Awaker() {
        timer = new Timer(1000, e1 -> System.out.println(player.getPosition()));
        //timer.start();
        Server server = new Server(this);

        InputStream is;
        try {
            is = new FileInputStream("media/music.mp3");
            player = new CustomPlayer(this, is);
            player.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    try {
                        player.resume();
                    } catch (JavaLayerException e1) {
                        e1.printStackTrace();
                    }
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

        //g.setColor(ColorTranslator.translateGewichtet(list));

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
    public void newSamples(short[] samples) {
        analyzer.pushSamples(samples);
    }

    @Override
    public void playbackStarted() {

    }

    @Override
    public void playbackFinished() {

    }

    @Override
    public void playbackStopped() {

    }

    @Override
    public void playbackPaused() {

    }


    @Override
    public void newResults(List<Map.Entry<Double, Double>> list) {
        this.list = list;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public boolean playFile(String name) {
        return false;
    }

    @Override
    public void play() {
        try {
            player.play();
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playFromPosition(int position) {

    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void setBrightness(int brightness) {

    }

    @Override
    public void changeVisualisation(String newType) {

    }

    @Override
    public String getStatus() {
        return player.getStatus().toString();
    }
}
