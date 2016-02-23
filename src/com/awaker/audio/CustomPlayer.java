package com.awaker.audio;

import com.awaker.Awaker;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.io.InputStream;

@SuppressWarnings("Duplicates")
public class CustomPlayer {
    /**
     * The MPEG audio bitstream.
     */private Bitstream bitstream;

    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;

    /**
     * The AudioDevice the audio samples are written to.
     */
    private AudioDevice audio;

    /**
     * Has the player been closed?
     */
    private boolean closed = false;

    /**
     * Has the player played back all frames from the stream?
     */
    private boolean complete = false;

    private int lastPosition = 0;

    private Awaker awaker;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public CustomPlayer(InputStream stream, Awaker a) throws JavaLayerException {
        this(stream, null, a);
    }

    public CustomPlayer(InputStream stream, AudioDevice device, Awaker a) throws JavaLayerException {
        awaker = a;
        bitstream = new Bitstream(stream);
        decoder = new Decoder();

        if (device != null) {
            audio = device;
        } else {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        }
        audio.open(decoder);
    }

    public void play() throws JavaLayerException {
        play(Integer.MAX_VALUE);
    }

    /**
     * Plays a number of MPEG audio frames.
     *
     * @param frames The number of frames to play.
     * @return true if the last frame was played, or false if there are more frames.
     */
    public boolean play(int frames) throws JavaLayerException {
        boolean ret = true;

        while (frames-- > 0 && ret) {
            ret = decodeFrame();
        }

        if (!ret) {
            // last frame, ensure all data flushed to the audio device.
            AudioDevice out = audio;
            if (out != null) {
                out.flush();
                synchronized (this) {
                    complete = !closed;
                    close();
                }
            }
        }
        return ret;
    }

    /**
     * Cloases this player. Any audio currently playing is stopped immediately.
     */
    public synchronized void close() {
        AudioDevice out = audio;
        if (out != null) {
            closed = true;
            audio = null;
            // this may fail, so ensure object state is set up before
            // calling this method.
            out.close();
            lastPosition = out.getPosition();
            try {
                bitstream.close();
            } catch (BitstreamException ex) {
            }
        }
    }

    /**
     * Returns the completed status of this player.
     *
     * @return true if all available MPEG audio frames have been decoded, or false otherwise.
     */
    public synchronized boolean isComplete() {
        return complete;
    }

    /**
     * Retrieves the position in milliseconds of the current audio sample being played. This method delegates to the
     * <code> AudioDevice</code> that is used by this player to sound the decoded audio samples.
     */
    public int getPosition() {
        int position = lastPosition;

        AudioDevice out = audio;
        if (out != null) {
            position = out.getPosition();
        }
        return position;
    }

    /**
     * Decodes a single frame.
     *
     * @return true if there are no more frames to decode, false otherwise.
     */
    protected boolean decodeFrame() throws JavaLayerException {
        try {
            AudioDevice out = audio;
            if (out == null)
                return false;

            Header h = bitstream.readFrame();

            if (h == null)
                return false;

            // sample buffer set when decoder constructed
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);

            awaker.newSamples(output.getBuffer());

            synchronized (this) {
                out = audio;
                if (out != null) {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }
            bitstream.closeFrame();
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }


}