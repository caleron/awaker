package com.awaker;

import com.awaker.analyzer.ResultListener;
import com.awaker.audio.CustomPlayer;
import com.awaker.analyzer.FFTAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Awaker extends JPanel implements ResultListener {

    List<Map.Entry<Double, Double>> list;

    FFTAnalyzer analyzer = new FFTAnalyzer(this);

    Timer timer;

    CustomPlayer player;

    public Awaker() {
        timer = new Timer(1000, e1 -> System.out.println(player.getPosition()));
        //timer.start();

        new Thread(() -> {
            InputStream is = null;
            try {
                is = new FileInputStream("media/furelise.mp3");
                player = new CustomPlayer(is, this);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

    public void newSamples(short[] samples) {
        analyzer.pushSamples(samples);
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
