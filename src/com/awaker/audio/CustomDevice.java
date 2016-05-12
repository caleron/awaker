package com.awaker.audio;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDeviceBase;

import javax.sound.sampled.*;

class CustomDevice extends AudioDeviceBase {

    private SourceDataLine source = null;

    private AudioFormat fmt = null;

    private boolean gainControlSupported = false;

    private byte[] byteBuf = new byte[4096];

    private AudioFormat getAudioFormat() {
        if (fmt == null) {
            Decoder decoder = getDecoder();
            fmt = new AudioFormat(decoder.getOutputFrequency(),
                    16,
                    decoder.getOutputChannels(),
                    true,
                    false);
        }
        return fmt;
    }

    protected void openImpl() throws JavaLayerException {
    }


    // createSource fix.
    private void createSource() throws JavaLayerException {
        Throwable t = null;
        try {

            Mixer.Info[] arrMixerInfo = AudioSystem.getMixerInfo();
            Mixer.Info mixerInfo = arrMixerInfo[0];

            for (Mixer.Info info : arrMixerInfo) {
                if (info.getName().toLowerCase().contains("usb")) {
                    mixerInfo = info;
                    break;
                }
            }

            Line line = AudioSystem.getSourceDataLine(getAudioFormat(), mixerInfo);
            if (line != null) {
                source = (SourceDataLine) line;
                //source.open(fmt, millisecondsToBytes(fmt, 2000));
                source.open(fmt);

                gainControlSupported = source.isControlSupported(FloatControl.Type.MASTER_GAIN);

                source.start();
            }
        } catch (RuntimeException | LinkageError | LineUnavailableException ex) {
            t = ex;
        }
        if (source == null) throw new JavaLayerException("cannot obtain source audio line", t);
    }

    protected void closeImpl() {
        if (source != null) {
            source.close();
        }
    }

    protected void writeImpl(short[] samples, int offs, int len)
            throws JavaLayerException {
        if (source == null)
            createSource();

        byte[] b = toByteArray(samples, offs, len);
        source.write(b, 0, len * 2);
    }

    private byte[] getByteArray(int length) {
        if (byteBuf.length < length) {
            byteBuf = new byte[length + 1024];
        }
        return byteBuf;
    }

    private byte[] toByteArray(short[] samples, int offs, int len) {
        byte[] b = getByteArray(len * 2);
        int idx = 0;
        short s;
        while (len-- > 0) {
            s = samples[offs++];
            b[idx++] = (byte) s;
            b[idx++] = (byte) (s >>> 8);
        }
        return b;
    }

    protected void flushImpl() {
        if (source != null) {
            source.drain();
        }
    }

    public int getPosition() {
        int pos = 0;
        if (source != null) {
            pos = (int) (source.getMicrosecondPosition() / 1000);
        }
        return pos;
    }

    /**
     * Setzt die Lautstärke. Hat erst nach öffnen der Line einen Effekt.
     *
     * @param value Wert zwischen 0 und 100
     */
    void setVolume(int value) {
        if (gainControlSupported && source != null) {
            FloatControl c = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
            float range = c.getMaximum() - c.getMinimum();

            float factor = (float) Math.log10(value);

            float newValue = c.getMinimum() + range * (factor / 2f);

            c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), newValue)));
        }
    }
}
