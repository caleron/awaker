package com.awaker;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Test2 {

    public static void main(String[] args) {
        Test2 t = new Test2();
        t.testPlay("media/music.mp3");
    }

    public void testPlay(String mp3) {
        try {
            File file = new File(mp3);
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            AudioInputStream din = null;
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);

            play(decodedFormat, din);
            //spi(decodedFormat, in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void play(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);

        int nBytesRead = 0, nBytesWritten = 0;
        while (nBytesRead != -1) {
            nBytesRead = din.read(data, 0, data.length);
            if (nBytesRead != -1) {
                nBytesWritten = line.write(data, 0, nBytesRead);
                out.write(data, 0, nBytesRead);
            }

            line.drain();
            line.stop();
            line.close();
            din.close();
        }

        byte[] audio = out.toByteArray();

    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }
}
