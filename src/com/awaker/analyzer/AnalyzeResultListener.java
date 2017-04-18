package com.awaker.analyzer;

import java.util.List;
import java.util.Map;

/**
 * Interface for classes to listen to new results of the SampleAnalyzer.
 */
@FunctionalInterface
public interface AnalyzeResultListener {
    /**
     * Will be called each time a new batch of samples has been analyzed.
     *
     * @param list A list with pairs of frequency and amplitude.
     */
    void newResults(List<Map.Entry<Double, Double>> list);
}
