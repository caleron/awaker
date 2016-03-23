package com.awaker.analyzer;

import com.awaker.Awaker;

import java.awt.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"Duplicates", "unused"})
public class ColorTranslator {

    public static Color translateMaximum(List<Map.Entry<Double, Double>> list) {
        Map.Entry<Double, Double> maxAmp = findMaxima(list);

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

        //System.out.println(list.size() + ": hue: " + hue + ", brightness: " + brightness);

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

        //System.out.println(list.size() + ": hue: " + hue + ", brightness: " + brightness);

        return Color.getHSBColor(1 - hue, 1, brightness);
    }

    public static Color translatePartition(List<Map.Entry<Double, Double>> list) {
        if (list == null || list.isEmpty())
            return Color.black;

        final int freqDivider1 = 200;
        final int freqDivider2 = 1000;
        List<Map.Entry<Double, Double>> partition1 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() < freqDivider1).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition2 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() > freqDivider1 && entry.getKey() < freqDivider2).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition3 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() > freqDivider2).collect(Collectors.toList()));

        Map.Entry<Double, Double> max1 = findMaxima(partition1);
        Map.Entry<Double, Double> max2 = findMaxima(partition2);
        Map.Entry<Double, Double> max3 = findMaxima(partition3);

        float red = Math.min((float) Math.pow(max1.getValue() / 13000.0, 2), 1);
        float green = Math.min((float) Math.pow(max2.getValue() / 8000.0, 1.5) + 0.2f, 1);
        float blue = Math.min((float) Math.pow(max3.getValue() / 7000.0, 1.5) + 0.2f, 1);

        if (Awaker.isMSWindows) {
            System.out.println("red = " + red + " green = " + green + " blue = " + blue
                    + " band1max = " + max1.getValue() + " band2max = " + max2.getValue() + " band3max = " + max3.getValue());
        }

        return new Color(red, green, blue);
    }

    public static Color translatePartition2(List<Map.Entry<Double, Double>> list) {
        if (list == null || list.isEmpty())
            return Color.black;

        final int freqDivider1 = 200;
        final int freqDivider2 = 800;
        final int freqDivider3 = 1500;

        List<Map.Entry<Double, Double>> partition1 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() < freqDivider1).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition2 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() > freqDivider1 && entry.getKey() < freqDivider2).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition3 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() > freqDivider2 && entry.getKey() < freqDivider3).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition4 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() > freqDivider3).collect(Collectors.toList()));

        Map.Entry<Double, Double> max1 = findMaxima(partition1);
        Map.Entry<Double, Double> max2 = findMaxima(partition2);
        Map.Entry<Double, Double> max3 = findMaxima(partition3);
        Map.Entry<Double, Double> max4 = findMaxima(partition4);

        float red = cap((float) Math.pow(max1.getValue() / 13000.0, 2));
        float green = (float) (max2.getValue() / 15000.0);
        float blue = (float) (max4.getValue() / 8000.0);

        int range = freqDivider3 - freqDivider2;

        green += (1 - ((max3.getKey() - freqDivider2) / range)) * (max3.getValue() / 15000.0);
        blue += ((max3.getKey() - freqDivider2) / range) * (max3.getValue() / 7000.0);

        green = cap(Math.pow(green, 2));
        blue = cap(Math.pow(blue, 2));

        /*if (Awaker.isMSWindows) {
            System.out.println("red = " + red + " green = " + green + " blue = " + blue
                    + " band1max = " + max1.getValue() + " band2max = " + max2.getValue() + " band3max = " + max3.getValue());
        }*/

        return new Color(red, green, blue);
    }


    public static Color translatePartitionAndGewichtet(List<Map.Entry<Double, Double>> list) {
        if (list == null || list.isEmpty())
            return Color.black;

        final int freqDivider1 = 200;
        List<Map.Entry<Double, Double>> partition1 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() < freqDivider1).collect(Collectors.toList()));
        List<Map.Entry<Double, Double>> partition2 = new ArrayList<>(
                list.stream().filter(entry -> entry.getKey() >= freqDivider1).collect(Collectors.toList()));

        Map.Entry<Double, Double> max1 = findMaxima(partition1);

        float red = Math.min((float) Math.pow(max1.getValue() / 13000.0, 2), 1);

        Color color2 = translateGewichtet(partition2);

        if (Awaker.isMSWindows) {
            System.out.println("red = " + red + " green = " + color2.getGreen() + " blue = " + color2.getBlue());
        }

        return new Color(red, color2.getGreen() / 255f, color2.getBlue() / 255f);
    }

    private static Map.Entry<Double, Double> findMaxima(List<Map.Entry<Double, Double>> list) {
        Map.Entry<Double, Double> maxAmp = new AbstractMap.SimpleEntry<>(0.0, 0.0);

        //Maximalen Ausschlag bestimmen
        for (Map.Entry<Double, Double> entry : list) {
            if (maxAmp.getValue() < entry.getValue()) {
                maxAmp = entry;
            }
        }
        return maxAmp;
    }

    private static float cap(float f) {
        return Math.max(0, Math.min(f, 1));
    }
    private static float cap(double f) {
        return (float) Math.max(0, Math.min(f, 1));
    }
}
