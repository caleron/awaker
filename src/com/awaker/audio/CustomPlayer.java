package com.awaker.audio;

import com.awaker.util.Log;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

class CustomPlayer implements PlayerListener {
    /**
     * The MPEG audio bitstream.
     */
    private final Bitstream bitstream;

    /**
     * The MPEG audio decoder.
     */
    private final Decoder decoder;

    /**
     * The AudioDevice the audio samples are written to.
     */
    private CustomDevice audio;

    private int lastPosition = 0;

    /**
     * Die Anzahl ms, die abgespielt oder übersprungen wurden, bevor pausiert wurde. Dies ist notwendig, da beim
     * Schließen und erneuten Öffnen des AudioDevice als Position die bis dahin auf der Instanz abgespielte Zeit
     * zurückgegeben wird. Folglich wird die Position bei jedem Pausieren zurückgesetzt.
     */
    private int offsetPlayedMs = 0;

    private final List<PlayerListener> samplesListener;

    private PlaybackStatus status = PlaybackStatus.CREATED;

    private Header lastHeader;
    private boolean sampleRateReported = false;

    private int volume;

    /**
     * Creates a new <code>Player</code> instance.
     *
     * @param listener PlayerListener, der über Playback-Events informiert wird
     * @param stream   Inputstream, aus dem abgespielt werden soll.
     */
    CustomPlayer(PlayerListener[] listener, InputStream stream, int volume) {
        samplesListener = Arrays.asList(listener);
        decoder = new Decoder();
        bitstream = new Bitstream(stream);
        this.volume = volume;
    }

    /**
     * Öffnet das Audiogerät
     */
    private synchronized void openAudio() throws JavaLayerException {
        if (audio == null) {
            //FactoryRegistry r = FactoryRegistry.systemRegistry();
            //audio = r.createAudioDevice();
            audio = new CustomDevice(volume);
            audio.open(decoder);
        }
    }

    /**
     * Spielt die aktuelle Datei ab oder setzt diese fort, falls pausiert wurde.
     */
    void play() throws JavaLayerException {
        if (status == PlaybackStatus.PAUSED) {
            resume();
        } else {
            play(0);
        }
    }

    /**
     * Spielt die aktuelle Datei ab einem bestimmten Frame ab.
     *
     * @param start The first frame to play
     */
    private void play(final int start) throws JavaLayerException {
        if (status == PlaybackStatus.PLAYING)
            return;

        if (status == PlaybackStatus.STOPPED) {
            //Wenn completed oder stopped, dann ist der Bitstream schon geschlossen
            throw new JavaLayerException("Can't start playback: Bitstream already closed.");
        }

        openAudio();
        /*
          Muss hier schon gesetzt werden, damit der Status nach Ausführung dieser Methode auf PLAYING ist,
          ansonsten bekommt die App eine falsche Antwort.
         */
        status = PlaybackStatus.PLAYING;
        Thread playerThread = new Thread(() -> {
            try {
                runPlayback(start);
            } catch (JavaLayerException e) {
                Log.error(e);
            }
        });
        playerThread.start();
    }

    /**
     * Beginnt die Wiedergabe ab einer bestimmten Position
     *
     * @param targetSecond Die Position in Sekunden
     */
    void playFromPosition(int targetSecond) throws JavaLayerException {
        lastHeader = bitstream.readFrame();
        float msPerFrame = (int) lastHeader.ms_per_frame();

        int skipCount = (int) ((targetSecond * 1000f) / msPerFrame);
        offsetPlayedMs = targetSecond * 1000;

        play(skipCount);
    }


    /**
     * Spielt die aktuelle Datei ab. Blockiert die Ausführung, sollte also in einem eigenen Thread ausgeführt werden.
     *
     * @param start Die Anzahl an Frames, die übersprungen werden sollen.
     */
    private void runPlayback(final int start) throws JavaLayerException {
        playbackStarted(getPosition());

        skipFrames(start);

        boolean ret = true;
        while (ret) {
            ret = decodeFrame();
        }

        AudioDevice out = audio;
        if (out != null) {
            out.flush();
            synchronized (this) {
                status = PlaybackStatus.STOPPED;
                close();
            }
            //Wenn out schon vorher null ist, dann wurde gestoppt, deshalb nur Event auslösen, wenn out noch nicht null war
            playbackFinished();
        }
    }

    /**
     * Stoppt die Wiedergabe und schließt alle verbundenen Streams.
     */
    void stop() {
        status = PlaybackStatus.STOPPED;
        playbackStopped();
        if (audio != null) {
            offsetPlayedMs += audio.getPosition();
        }

        close();
    }

    /**
     * Pausiert die Wiedergabe.
     */
    synchronized void pause() {
        status = PlaybackStatus.PAUSED;
        playbackPaused();

        AudioDevice out = audio;
        if (out != null) {
            lastPosition = out.getPosition();
            offsetPlayedMs += lastPosition;

            audio = null;
            out.close();
        }
    }

    /**
     * Setzt die Wiedergabe fort.
     */
    private void resume() throws JavaLayerException {
        if (status != PlaybackStatus.PAUSED)
            return;

        openAudio();
        play(0);
    }

    /**
     * Schließt den Player. Eine aktuelle Wiedergabe wird sofort abgebrochen.
     */
    private synchronized void close() {
        AudioDevice out = audio;
        if (out != null) {
            audio = null;
            // this may fail, so ensure object state is set up before
            // calling this method.
            out.close();
            lastPosition = out.getPosition();
            offsetPlayedMs += lastPosition;
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
    boolean isPlaying() {
        return status == PlaybackStatus.PLAYING;
    }

    /**
     * Gibt den Playback-Status wieder.
     *
     * @return Playback-Status
     */
    PlaybackStatus getStatus() {
        return status;
    }

    /**
     * Retrieves the position in milliseconds of the current audio sample being played. This method delegates to the
     * <code> AudioDevice</code> that is used by this player to sound the decoded audio samples.
     */
    int getPosition() {
        AudioDevice out = audio;
        if (out != null) {
            //Falls gerade abgespielt wird, wurden die
            return offsetPlayedMs + out.getPosition();
        }

        return offsetPlayedMs;
    }

    int getOffsetPlayedMs() {
        return offsetPlayedMs;
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

            lastHeader = bitstream.readFrame();

            if (lastHeader == null)
                return false;

            if (!sampleRateReported) {
                //lastHeader ist jetzt != null
                reportAudioParams(getSampleRate(), lastHeader.ms_per_frame());
                sampleRateReported = true;
            }

            // sample buffer set when decoder constructed
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(lastHeader, bitstream);

            newSamples(output.getBuffer());

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
     * Überspringt die angegebene Zahl an Frames
     *
     * @param count Anzahl zu überspringender Frames
     * @return False, wenn das Ende des Streams erreicht wurde
     */
    private boolean skipFrames(int count) throws JavaLayerException {
        boolean ret = true;
        while (count-- > 0 && ret)
            ret = skipFrame();

        return ret;
    }

    /**
     * skips over a single frame
     *
     * @return false if there are no more frames to decode, true otherwise.
     */
    private boolean skipFrame() throws JavaLayerException {
        lastHeader = bitstream.readFrame();

        if (lastHeader == null)
            return false;

        if (!sampleRateReported) {
            //lastHeader ist jetzt != null
            reportAudioParams(getSampleRate(), lastHeader.ms_per_frame());
            sampleRateReported = true;
        }

        bitstream.closeFrame();
        return true;
    }

    int getSampleRate() {
        if (lastHeader != null) {
            return lastHeader.frequency();
        }
        return 0;
    }

    void setVolume(int volume) {
        this.volume = volume;
        if (audio != null) {
            audio.setVolume(volume);
        }
    }

    @Override
    public void newSamples(short[] samples) {
        samplesListener.forEach(playerListener -> playerListener.newSamples(samples));
    }

    @Override
    public void reportAudioParams(int sampleRate, float msPerFrame) {
        samplesListener.forEach(playerListener -> playerListener.reportAudioParams(sampleRate, msPerFrame));
    }

    public void playbackStarted(int positionMs) {
        samplesListener.forEach(playerListener -> playerListener.playbackStarted(positionMs));
    }

    public void playbackFinished() {
        samplesListener.forEach(PlayerListener::playbackFinished);
    }

    public void playbackStopped() {
        samplesListener.forEach(PlayerListener::playbackStopped);
    }

    public void playbackPaused() {
        samplesListener.forEach(PlayerListener::playbackPaused);
    }
}