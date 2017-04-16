package com.awaker.analyzer.aot;

import com.awaker.analyzer.ColorTranslator;
import com.awaker.analyzer.SampleAnalyzer;
import com.awaker.analyzer.SampleQuantizer;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import javazoom.jl.decoder.*;

import java.awt.*;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadedAotAnalyzer {

    private int sampleRate;

    private final SampleQuantizer quantizer;
    //    private List<Byte> outputArray;
    private int[] outputArray;

    private final ConcurrentLinkedQueue<Map.Entry<Integer, short[]>> sampleQueue;
    private int sampleCounter;
    private final ConcurrentLinkedQueue<Map.Entry<Integer, List<Map.Entry<Double, Double>>>> transformedQueue;

    private boolean decodingFinished = false;

    private Thread transformThread1;
    private Thread transformThread2;
    private Thread transformThread3;
    private Thread transformThread4;
    private Thread translateThread;

    private AtomicInteger transformCount = new AtomicInteger(0);
    private AtomicInteger translatedCount = new AtomicInteger(0);
    private AtomicInteger quantizedCount = new AtomicInteger(0);

//    private HashMap<Integer, List<Map.Entry<Double, Double>>> analyzedAudio;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public ThreadedAotAnalyzer() {
        quantizer = new SampleQuantizer(2);
//        analyzedAudio = new HashMap<>(10000);

        sampleQueue = new ConcurrentLinkedQueue<>();
        transformedQueue = new ConcurrentLinkedQueue<>();
    }

    public boolean analyze(TrackWrapper track) {
        return analyze(MediaManager.getFileStream(track));
    }

    public boolean analyze(InputStream inputStream) {
        decodingFinished = false;
        sampleCounter = 0;

        translatedCount.set(0);
        transformCount.set(0);
        quantizedCount.set(0);

//        analyzedAudio.clear();
        sampleQueue.clear();
        transformedQueue.clear();

        transformThread1 = new Thread(this::transform);
        transformThread1.start();
        transformThread2 = new Thread(this::transform);
        transformThread2.start();
        transformThread3 = new Thread(this::transform);
        transformThread3.start();
        transformThread4 = new Thread(this::transform);
        transformThread4.start();
        translateThread = new Thread(this::translate);
        translateThread.start();

        decodeAndQuantize(inputStream);
        decodingFinished = true;
        try {
            transformThread1.join();
            transformThread2.join();
            transformThread3.join();
            transformThread4.join();
            translateThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("multithreaded result size: " + (translatedCount.get() * 4));
        System.out.println("transformed: " + transformCount.get());
        System.out.println("translated: " + translatedCount.get());
        System.out.println("decoded/quantized: " + quantizedCount.get());
//        return outputArray;
        return translatedCount.get() == transformCount.get() && transformCount.get() == quantizedCount.get();
    }

    /**
     * Decodes a single frame.
     */
    private void decodeAndQuantize(InputStream stream) {
        try {
            Decoder decoder = new Decoder();
            Bitstream bitstream = new Bitstream(stream);

            outputArray = new int[10000];

            //read first frame
            Header header = bitstream.readFrame();
            sampleRate = header.frequency();
            //put in samplerate
            outputArray[0] = sampleRate;

            while (header != null) {
                //decode next frame
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                short[] samples = output.getBuffer();

                quantizeSamples(samples);

                //close the frame
                bitstream.closeFrame();

                //read next frame
                header = bitstream.readFrame();
            }
        } catch (JavaLayerException ex) {
            throw new RuntimeException("error decoding file");
        }
    }


    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    private void quantizeSamples(short[] samples) {
        short[] quantizedSamples = quantizer.quantize(samples);
        if (quantizedSamples.length == 2048) {
            sampleQueue.add(new AbstractMap.SimpleEntry<>(sampleCounter++, quantizedSamples));
            quantizedCount.incrementAndGet();

        } else if (quantizedSamples.length == 4096) {
            sampleQueue.add(new AbstractMap.SimpleEntry<>(sampleCounter++, Arrays.copyOfRange(quantizedSamples, 0, 2048)));
            sampleQueue.add(new AbstractMap.SimpleEntry<>(sampleCounter++, Arrays.copyOfRange(quantizedSamples, 2048, 4096)));

            quantizedCount.incrementAndGet();
            quantizedCount.incrementAndGet();
        } else if (quantizedSamples.length > 4096) {
            throw new RuntimeException("unknown sample count" + quantizedSamples.length);
        }
    }

    //Samples für einen Channel, also insgesamt 2048 werden gebraucht

    private void transform() {
        try {

            //analyze samples
            while (!decodingFinished || !sampleQueue.isEmpty()) {
                Map.Entry<Integer, short[]> samples = sampleQueue.poll();

                if (samples == null) {
                    Thread.sleep(1);
                    continue;
                }
                transformedQueue.add(new AbstractMap.SimpleEntry<>(samples.getKey(),
                        SampleAnalyzer.analyzeSamples(samples.getValue(), 2, sampleRate)));
                transformCount.incrementAndGet();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void translate() {
        try {
            while (transformThread1.isAlive() || transformThread2.isAlive() || transformThread3.isAlive()
                    || transformThread4.isAlive() || !transformedQueue.isEmpty()) {
                Map.Entry<Integer, List<Map.Entry<Double, Double>>> entry = transformedQueue.poll();

                if (entry == null) {
                    Thread.sleep(1);
                    continue;
                }

                //translate to color
                Color color = ColorTranslator.translatePartition2(entry.getValue());
                //write color

                if (entry.getKey() > outputArray.length - 1) {
                    expandOutputArray(entry.getKey());
                }
                outputArray[entry.getKey()] = color.getRGB();
                translatedCount.incrementAndGet();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void expandOutputArray(int newIndex) {
        int[] tmpArray = outputArray;

        int newLenght = (int) Math.max(tmpArray.length * 2, newIndex * 1.5);
        outputArray = new int[newLenght];
        System.arraycopy(tmpArray, 0, outputArray, 0, tmpArray.length);
    }

//    private synchronized void addInt(int value) {
//        outputArray.add((byte) (value >>> 24));
//        outputArray.add((byte) (value >>> 16));
//        outputArray.add((byte) (value >>> 8));
//        outputArray.add((byte) value);
//    }
}
