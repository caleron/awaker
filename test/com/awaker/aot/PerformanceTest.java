package com.awaker.aot;

import com.awaker.analyzer.aot.PreAnalyzer;
import com.awaker.analyzer.aot.ThreadedAotAnalyzer;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 * Compares the performance of the single-threaded and the multi-threaded analyzer.
 */
public class PerformanceTest {
    //around 10.5s
    @Test
    public void testSingleThread() {
        TrackWrapper track = new TrackWrapper(0, "", "", "", "media\\N.W.A - Straight Outta Compton.mp3", new Date(), 259);

        InputStream stream = MediaManager.getFileStream(track);
        PreAnalyzer preAnalyzer = new PreAnalyzer();
        long start = System.currentTimeMillis();
        preAnalyzer.analyze(stream);

        System.out.println((System.currentTimeMillis() - start));
    }

    //around 2.9s
    @Test
    public void testMultiThread() {

        TrackWrapper track = new TrackWrapper(0, "", "", "", "media\\N.W.A - Straight Outta Compton.mp3", new Date(), 259);

        InputStream stream = MediaManager.getFileStream(track);
        ThreadedAotAnalyzer preAnalyzer = new ThreadedAotAnalyzer();

        long start = System.currentTimeMillis();

        assertTrue(preAnalyzer.analyze(stream));

        System.out.println((System.currentTimeMillis() - start));
    }
}
