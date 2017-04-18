package com.awaker.analyzer;

/**
 * Quantizes samples. Takes samples from decoding and returns them in array sizes with a power of 2, so that the array
 * is suitable for FFT analysis. Buffers overlapping samples.
 */
public class SampleQuantizer {

    private int bufferedSampleCount = 0;
    //1024 Samples entsprechen bei 44100Hz Abtastrate etwa 23ms
    //Samples jeweils für einen Channel, also insgesamt 2048 werden gebraucht
    private static final int MIN_ANALYZE_SIZE = 1024;
    private final int channels;
    private final short[] buffer;

    /**
     * Creates a new {@link SampleQuantizer}.
     *
     * @param channels the channel count
     */
    public SampleQuantizer(int channels) {
        this.channels = channels;

        int bufferSize = MIN_ANALYZE_SIZE * channels;

        buffer = new short[bufferSize];
    }

    /**
     * Takes a sample array and returns an array which size is a power of 2. (1024 or 2048, multiplied by channel count)
     *
     * @param samples input samples
     * @return short array with size equals 1024 or 2048 multiplied by channel count
     */
    public short[] quantize(short[] samples) {
        if (bufferedSampleCount + samples.length >= MIN_ANALYZE_SIZE * channels) {
            int newSamplesCount;
            short[] analyzeSamples;

            if (bufferedSampleCount + samples.length >= MIN_ANALYZE_SIZE * channels * 2) {
                analyzeSamples = new short[1024 * 2 * channels];
                newSamplesCount = MIN_ANALYZE_SIZE * channels * 2 - bufferedSampleCount;
            } else {
                analyzeSamples = new short[1024 * channels];
                newSamplesCount = MIN_ANALYZE_SIZE * channels - bufferedSampleCount;
            }

            //Samples aus dem buffer schreiben
            System.arraycopy(buffer, 0, analyzeSamples, 0, bufferedSampleCount);
            //Samples im Buffer = bufferedSampleCount

            //Neue Samples schreiben
            System.arraycopy(samples, 0, analyzeSamples, bufferedSampleCount, newSamplesCount);
            //Samples im Buffer = bufferedSampleCount + newSamplescount = bufferedSampleCount + 1024*2 - bufferedSampleCount
            //  = 1024*2 (oder 1024 * 2 * 2) 1024 hier als MIN_ANALYZE_SIZE

            //Überschüssige neue Samples in den buffer schreiben
            //Alle Samples aus dem Buffer wurden verwendet, deshalb alte überschreiben
            bufferedSampleCount = samples.length - newSamplesCount;
            System.arraycopy(samples, newSamplesCount, buffer, 0, bufferedSampleCount);

            return analyzeSamples;
        } else {
            //Nicht genug Samples für eine Analyse da, neue Samples in Buffer schreiben
            System.arraycopy(samples, 0, buffer, bufferedSampleCount, samples.length);
            bufferedSampleCount += samples.length;
        }
        return null;
    }
}
