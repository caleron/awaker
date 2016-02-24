package com.awaker.audio;

import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.io.InputStream;

@SuppressWarnings("Duplicates")
public class CustomPlayer {
    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;

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

    private boolean isPlaying = false;

    private int lastPosition = 0;

    private PlayerListener samplesListener;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public CustomPlayer(PlayerListener a) throws JavaLayerException {
        samplesListener = a;
        decoder = new Decoder();

        openAudio();
    }

    public synchronized void openAudio() throws JavaLayerException {
        if (audio == null) {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        }
        audio.open(decoder);
    }

    public synchronized void setStream(InputStream stream) {
        bitstream = new Bitstream(stream);
    }

    /**
     * Spielt die aktuelle Datei ab.
     *
     * @return true if the last frame was played, or false if there are more frames.
     */
    public void play() throws JavaLayerException {
        play(0);
    }

    /**
     * Spielt die aktuelle Datei ab einem bestimmten Frame ab.
     *
     * @param start The first frame to play
     */
    public void play(final int start) throws JavaLayerException {
        new Thread(() -> {
            try {
                runPlayback(start);
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runPlayback(final int start) throws JavaLayerException {
        isPlaying = true;
        samplesListener.playbackStarted();

        boolean ret = true;
        int offset = start;
        while (offset-- > 0 && ret)
            ret = skipFrame();

        //ret = true;

        while (ret) {
            ret = decodeFrame();
        }

        //if (!ret) {
        // last frame, ensure all data flushed to the audio device.
        AudioDevice out = audio;
        if (out != null) {
            out.flush();
            synchronized (this) {
                complete = !closed;
                close();
            }
        }
        //}
        samplesListener.playbackFinished();
    }

    /**
     * closes the player and notifies <code>PlaybackListener</code>
     */
    public void stop() {
        samplesListener.playbackStopped();
        close();
    }

    public void pause() {
        samplesListener.playbackPaused();
        isPlaying = false;

        AudioDevice out = audio;
        if (out != null) {
            audio = null;
            out.close();
        }
    }

    /**
     * Resumes the player.
     *
     * @throws JavaLayerException
     */
    public void resume() throws JavaLayerException {
        openAudio();
        play(0);
    }

    /**
     * Cloases this player. Any audio currently playing is stopped immediately.
     */
    public synchronized void close() {
        AudioDevice out = audio;
        if (out != null) {
            closed = true;
            isPlaying = false;
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

    public boolean isPlaying() {
        return isPlaying;
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

            samplesListener.newSamples(output.getBuffer());

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

    /**
     * skips over a single frame
     *
     * @return false    if there are no more frames to decode, true otherwise.
     */
    protected boolean skipFrame() throws JavaLayerException {
        Header h = bitstream.readFrame();

        if (h == null)
            return false;

        bitstream.closeFrame();
        return true;
    }
}