package com.awaker.analyzer;

import java.util.List;
import java.util.Map;

public interface AnalyzeResultListener {
    void newResults(List<Map.Entry<Double, Double>> list);
}
