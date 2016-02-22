package com.awaker;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class TestFFT {
    static final int sampleRate = 44100;
    static final int sampleFrame = 1024;
    static final int freq = 3000;
    static final int amp = 2000;

    //sinus verwendet Bogenmaß, also mit Pi bzw. Wellenlänge = 2*PI
    public static void main(String[] args) {

        double wellenlänge = sampleRate / freq;

        double[] samples = new double[sampleFrame];
        double[] imag = new double[sampleFrame];

        for (int i = 0; i < sampleFrame; i++) {
            samples[i] = (Math.sin((i / wellenlänge) * Math.PI * 2) * amp);
            imag[i] = 0;
        }

        FFT fft = new FFT(sampleFrame);
        fft.fft(samples, imag);

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();

        String ss = "";
        for (double re : samples) {
            ss += re + "\n";
        }

        ss += "\n\n";

        for (double im : imag) {
            ss += im + "\n";
        }

        StringSelection sel = new StringSelection(ss);
        cb.setContents(sel, sel);
    }

    private double computeFrequency(int arrayIndex) {
        return ((1.0 * sampleRate) / (1.0 * sampleFrame)) * arrayIndex;
    }
}
