package com.awaker;

import com.awaker.analyzer.FFTAnalyzer;
import com.awaker.analyzer.ResultListener;
import com.awaker.audio.CustomPlayer;
import com.awaker.audio.PlayerListener;
import javazoom.jl.decoder.JavaLayerException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker extends JPanel implements ResultListener, PlayerListener {

    List<Map.Entry<Double, Double>> list;

    FFTAnalyzer analyzer = new FFTAnalyzer(this);

    Timer timer;

    CustomPlayer player;

    public Awaker() {
        timer = new Timer(1000, e1 -> System.out.println(player.getPosition()));
        //timer.start();

        InputStream is = null;
        try {
            is = new FileInputStream("media/music.mp3");
            player = new CustomPlayer(this);
            player.setStream(is);
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
        g.setColor(Color.white);
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
}
