package com.awaker.audio_in;

import com.awaker.analyzer.AnalyzeResultListener;
import com.awaker.automation.EnvironmentEventListener;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ClapDetector implements AnalyzeResultListener {

    private final EnvironmentEventListener listener;
    private boolean lastSilent = false;
    private Map.Entry<Double, Double> lastLoud;
    private Long lastLoudTime = 0L;

    public ClapDetector(EnvironmentEventListener listener) {
        this.listener = listener;
    }

    public void newResults(List<Map.Entry<Double, Double>> list) {
        //niedrige Frequenzbereiche bis 600Hz entfernen (und damit auch Rauschen)
        while (list.get(0).getKey() < 600) {
            list.remove(0);
        }
        //alles über 6kHz entfernen
        while (list.get(list.size() - 1).getKey() > 6000) {
            list.remove(list.size() - 1);
        }

        //absteigend sortieren
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        if (list.get(0).getValue() < 50) {
            lastSilent = true;
        } else {
            if (lastSilent) {
                long nowTime = new Date().getTime();
                Map.Entry<Double, Double> loudestFreq = list.get(0);
                System.out.println(loudestFreq);
                if (nowTime - lastLoudTime < 1000) {
                    Double ampDiff = relativeDiff(lastLoud.getValue(), loudestFreq.getValue());
                    Double freqDiff = relativeDiff(lastLoud.getKey(), loudestFreq.getKey());
                    if (freqDiff < 2 && ampDiff < 2) {
                        listener.clapDetected();
                    }
                }

                lastLoud = list.get(0);
                lastLoudTime = new Date().getTime();
                lastSilent = false;
            }
        }
    }

    private static Double relativeDiff(Double val1, Double val2) {
        if (val1 > val2) {
            return Math.abs((val1 - val2) / val1);
        }
        return Math.abs((val2 - val1) / val2);
    }
}
