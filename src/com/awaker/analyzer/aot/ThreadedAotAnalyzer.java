package com.awaker.analyzer.aot;

import com.awaker.analyzer.ColorTranslator;
import com.awaker.analyzer.SampleAnalyzer;
import com.awaker.analyzer.SampleQuantizer;
import com.awaker.data.MediaManager;
import com.awaker.data.TrackWrapper;
import javazoom.jl.decoder.*;

import java.awt.*;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-threaded ahead of time frequency analyzer.
 */
public class ThreadedAotAnalyzer {
    public static final int ANALYZE_VERSION = 1;

    //the sample rate of the current track
    private int sampleRate;

    private final SampleQuantizer quantizer;
    //output array of integers
    private int[] outputArray;

    //queue with quantized samples
    private final ConcurrentLinkedQueue<Map.Entry<Integer, short[]>> sampleQueue;
    //the number of quantized sample arrays
    private int sampleCounter;
    //queue with transformed samples for translation
    private final ConcurrentLinkedQueue<Map.Entry<Integer, List<Map.Entry<Double, Double>>>> transformedQueue;

    //true if the decoding of the mp3 stream has finished
    private boolean decodingFinished = false;

    //threads for transforming and translating
    private Thread transformThread1;
    private Thread transformThread2;
    private Thread transformThread3;
    private Thread transformThread4;
    private Thread translateThread;

    //counter for checks (Atomic classes because of thread-safety
    private AtomicInteger transformCount = new AtomicInteger(0);
    private AtomicInteger translatedCount = new AtomicInteger(0);
    private AtomicInteger quantizedCount = new AtomicInteger(0);

//    private HashMap<Integer, List<Map.Entry<Double, Double>>> analyzedAudio;

    /**
     * Creates a new {@link ThreadedAotAnalyzer} instance with 2 channels.
     */
    public ThreadedAotAnalyzer() {
        quantizer = new SampleQuantizer(2);
//        analyzedAudio = new HashMap<>(10000);

        sampleQueue = new ConcurrentLinkedQueue<>();
        transformedQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Analyzes the given {@link TrackWrapper}s underlying mp3 song.
     *
     * @param track the track to analyze
     * @return true on success
     */
    public boolean analyze(TrackWrapper track) {
        return analyze(MediaManager.getFileStream(track));
    }

    /**
     * Analyzes the given {@link InputStream}s underlying mp3 song.
     *
     * @param inputStream the stream of the track to analyze
     * @return true on success
     */
    public boolean analyze(InputStream inputStream) {
        //reset counters and flags
        decodingFinished = false;
        sampleCounter = 0;

        translatedCount.set(0);
        transformCount.set(0);
        quantizedCount.set(0);

        //clear queues
        sampleQueue.clear();
        transformedQueue.clear();

        //start all threads
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

        //decode on current thread
        decodeAndQuantize(inputStream);
        decodingFinished = true;
        //wait for other threads to complete
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

        //return true if all steps handled the same amount
        return translatedCount.get() == transformCount.get() && transformCount.get() == quantizedCount.get();
    }

    public byte[] getOutputArray() {
        ByteBuffer buffer = ByteBuffer.allocate(outputArray.length * 4);
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(outputArray);
        return buffer.array();
    }

    /**
     * Decodes the given mp3 input stream and pushes the quantized samples into the analyze queue.
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

                //quantize the samples
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

    /**
     * Quantizes the given samples in arrays of 2048 shorts (1024 * channels) and adds them to the sampleQueue.
     *
     * @param samples non-quantized short array output from decoding
     */
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

    /**
     * Transforms samples from the sampleQueue by using the FFT and adds the results to the transformedQueue.
     */
    private void transform() {
        try {
            //analyze until decoding finished and sample queue is empty
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

    /**
     * Translates the analyze results from the transformedQueue to color values.
     */
    private void translate() {
        try {
            while (transformingRunning() || !transformedQueue.isEmpty()) {
                Map.Entry<Integer, List<Map.Entry<Double, Double>>> entry = transformedQueue.poll();

                if (entry == null) {
                    Thread.sleep(1);
                    continue;
                }

                //translate to color
                Color color = ColorTranslator.translatePartition2(entry.getValue());

                //ensure the capacity is sufficient for the index
                if (entry.getKey() > outputArray.length - 2) {
                    expandOutputArray(entry.getKey());
                }
                //write color to output array
                outputArray[entry.getKey() + 1] = color.getRGB();
                translatedCount.incrementAndGet();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns a boolean indicating the status of the transforming threads.
     *
     * @return true if one of the transformer threads is still running.
     */
    private boolean transformingRunning() {
        return transformThread1.isAlive() || transformThread2.isAlive() || transformThread3.isAlive()
                || transformThread4.isAlive();
    }

    /**
     * Expands the output array to ensure that the newIndex fits into it.
     *
     * @param newIndex the new index to access.
     */
    private void expandOutputArray(int newIndex) {
        int[] tmpArray = outputArray;

        //calculate new length
        int newLenght = (int) Math.max(tmpArray.length * 2, newIndex * 1.5);
        outputArray = new int[newLenght];
        //copy contents of old array to new array
        System.arraycopy(tmpArray, 0, outputArray, 0, tmpArray.length);
    }

//    private synchronized void addInt(int value) {
//        outputArray.add((byte) (value >>> 24));
//        outputArray.add((byte) (value >>> 16));
//        outputArray.add((byte) (value >>> 8));
//        outputArray.add((byte) value);
//    }
}
