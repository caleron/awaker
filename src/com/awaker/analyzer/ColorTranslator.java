package com.awaker.analyzer;

import java.awt.*;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("Duplicates")
public class ColorTranslator {

    public static Color translateMaximum(List<Map.Entry<Double, Double>> list) {
        Map.Entry<Double, Double> maxAmp = new AbstractMap.SimpleEntry<>(0.0, 0.0);

        //Maximalen Ausschlag bestimmen
        for (Map.Entry<Double, Double> entry : list) {
            if (maxAmp.getValue() < entry.getValue()) {
                maxAmp = entry;
            }
        }

        double freq = maxAmp.getKey();
        double amp = maxAmp.getValue();

        float brightness = Math.min((float) ((amp / 3000.0) + 0.2), 1);

        //float hue = (float) Math.max((Math.log10(freq) - 1) / Math.log10(22050), 0);
        float hue = (float) ((4 - Math.log10(freq)) / 2.37); //2.37 = log10(10000) - log10(43) = 4 - 1.63

        hue = Math.max(0, Math.min(1, hue));

        return Color.getHSBColor(1 - hue, 1, brightness);
    }

    public static Color translateDurchschnitt(List<Map.Entry<Double, Double>> list) {
        double hueSum = 0;
        double brightnessSum = 0;
        list = FFTAnalyzeThread.findLocalMaxima(list, 0.2);

        for (Map.Entry<Double, Double> entry : list) {
            double freq = entry.getKey();
            double amp = entry.getValue();

            brightnessSum += amp / 3000.0;

            float hue = (float) ((4 - Math.log10(freq)) / 2.37); //2.37 = log10(10000) - log10(43) = 4 - 1.63

            hueSum += Math.max(0, Math.min(1, hue));
        }

        float hue = (float) (hueSum / list.size());
        float brightness = (float) (brightnessSum / list.size());

        hue = Math.max(0, Math.min(1, hue));

        System.out.println(list.size() + ": hue: " + hue + ", brightness: " + brightness);

        return Color.getHSBColor(1 - hue, 1, brightness);
    }


    public static Color translateGewichtet(List<Map.Entry<Double, Double>> list) {
        if (list == null)
            return Color.BLACK;

        double hueSum = 0;
        double brightnessSum = 0;
        double ampSum = 0;
        list = FFTAnalyzeThread.findLocalMaxima(list, 0.2);

        for (Map.Entry<Double, Double> entry : list) {
            double freq = entry.getKey();
            double amp = entry.getValue();

            ampSum += amp;

            //brightnessSum += amp / 4000.0;
            brightnessSum += Math.pow(amp, 0.4) / Math.pow(4000, 0.4);

            float hue = (float) (((4 - Math.log10(freq)) / 2.37) * Math.pow(amp, 1.05)); //2.37 = log10(10000) - log10(43) = 4 - 1.63

            hueSum += hue; //Math.max(0, Math.min(1, hue));
        }

        float hue = (float) (hueSum / ampSum);
        float brightness = (float) (brightnessSum / list.size());

        hue = Math.max(0, Math.min(1, hue));

        System.out.println(list.size() + ": hue: " + hue + ", brightness: " + brightness);

        return Color.getHSBColor(1 - hue, 1, brightness);
    }
}
