package com.awaker.audio;

import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.io.InputStream;

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

    private int lastPosition = 0;

    private PlayerListener samplesListener;

    private PlaybackStatus status = PlaybackStatus.STOPPED;

    private Thread playerThread;

    /**
     * Creates a new <code>Player</code> instance.
     *
     * @param a      PlayerListener, der über Playback-Events informiert wird
     * @param stream Inputstream, aus dem abgespielt werden soll.
     * @throws JavaLayerException
     */
    public CustomPlayer(PlayerListener a, InputStream stream) throws JavaLayerException {
        samplesListener = a;
        decoder = new Decoder();
        bitstream = new Bitstream(stream);
    }

    /**
     * Öffnet das Audiogerät
     *
     * @throws JavaLayerException
     */
    private synchronized void openAudio() throws JavaLayerException {
        if (audio == null) {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
            audio.open(decoder);
        }
    }

    /**
     * Spielt die aktuelle Datei ab.
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
        if (status == PlaybackStatus.PLAYING)
            return;

        if (status == PlaybackStatus.COMPLETED) {
            //Wenn completed oder stopped, dann ist der Bitstream schon geschlossen
            throw new JavaLayerException("Can't start playback: Bitstream already closed.");
        }

        openAudio();
        playerThread = new Thread(() -> {
            try {
                runPlayback(start);
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        });
        playerThread.start();
    }

    //funktioniert nur, wenn noch nichts abgespielt wurde
    /*private void playFromPosition(int targetSecond) throws JavaLayerException {
        if (isPlaying()) {
            throw new JavaLayerException("cant seek while playing");
        }
        int positionInSeconds = lastPosition / 1000;
        Header h = bitstream.readFrame();
        int msPerFrame = (int) h.ms_per_frame();

        int distance = Math.abs(targetSecond - positionInSeconds);
        int skipCount = (distance * 1000) / msPerFrame;

        if (targetSecond > positionInSeconds) {
            play(skipCount);
        } else {
            play(-skipCount);
        }
    }*/

    /**
     * Überspringt die angegebene Zahl an Frames
     *
     * @param count Anzahl zu überspringender Frames
     * @return False, wenn das Ende des Streams erreicht wurde
     * @throws JavaLayerException
     */
    private boolean skipFrames(int count) throws JavaLayerException {
        boolean ret = true;
        if (count > 0) {
            while (count-- > 0 && ret)
                ret = skipFrame();
        } else {
            count = -count;
            while (count-- > 0)
                bitstream.unreadFrame();
        }
        return ret;
    }

    /**
     * Spielt die aktuelle Datei ab. Blockiert die Ausführung, sollte also in einem eigenen Thread ausgeführt werden.
     *
     * @param start Die Anzahl an Frames, die übersprungen werden sollen.
     * @throws JavaLayerException
     */
    private void runPlayback(final int start) throws JavaLayerException {
        status = PlaybackStatus.PLAYING;
        samplesListener.playbackStarted();

        skipFrames(start);

        boolean ret = true;
        while (ret) {
            ret = decodeFrame();
        }

        //if (!ret) {
        // last frame, ensure all data flushed to the audio device.
        AudioDevice out = audio;
        if (out != null) {
            out.flush();
            synchronized (this) {
                status = PlaybackStatus.COMPLETED;
                close();
            }
        }
        //}
        samplesListener.playbackFinished();
    }

    /**
     * Stoppt die Wiedergabe und schließt alle verbundenen Streams.
     */
    public void stop() {
        status = PlaybackStatus.STOPPED;
        samplesListener.playbackStopped();
        close();
    }

    /**
     * Pausiert die Wiedergabe.
     */
    public synchronized void pause() {
        status = PlaybackStatus.PAUSED;
        samplesListener.playbackPaused();

        AudioDevice out = audio;
        if (out != null) {
            lastPosition = out.getPosition();
            audio = null;
            out.close();
        }
    }

    /**
     * Setzt die Wiedergabe fort.
     *
     * @throws JavaLayerException
     */
    public void resume() throws JavaLayerException {
        if (status != PlaybackStatus.PAUSED)
            return;

        openAudio();
        play(0);
    }

    /**
     * Schließt den Player. Eine aktuelle Wiedergabe wird sofort abgebrochen.
     */
    public synchronized void close() {
        AudioDevice out = audio;
        if (out != null) {
            audio = null;
            // this may fail, so ensure object state is set up before
            // calling this method.
            out.close();
            lastPosition = out.getPosition();
            try {
                bitstream.close();
            } catch (BitstreamException ignored) {
            }
        }
    }

    /**
     * Gibt wieder, ob gerade abgespielt wird
     *
     * @return true, wenn abgespielt wird
     */
    public boolean isPlaying() {
        return status == PlaybackStatus.PLAYING;
    }

    /**
     * Returns the completed status of this player.
     *
     * @return true if all available MPEG audio frames have been decoded, or false otherwise.
     */
    public synchronized boolean isComplete() {
        return status == PlaybackStatus.COMPLETED;
    }

    /**
     * Gibt den Playback-Status wieder.
     *
     * @return Playback-Status
     */
    public PlaybackStatus getStatus() {
        return status;
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
    private boolean decodeFrame() throws JavaLayerException {
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
     * @return false if there are no more frames to decode, true otherwise.
     */
    private boolean skipFrame() throws JavaLayerException {
        Header h = bitstream.readFrame();

        if (h == null)
            return false;

        bitstream.closeFrame();
        return true;
    }
}