package org.kie.trustyai.metrics.utils;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;

public class PerColumnStatistics {
    java.util.Map<String, StatisticalSummaryValues> fitStats;

    public PerColumnStatistics(Map<String, StatisticalSummaryValues> fitStats) {
        this.fitStats = fitStats;
    }

    public Map<String, StatisticalSummaryValues> getFitStats() {
        return fitStats;
    }

    @Override
    public String toString() {
        return "PerColumnStatistics{" +
                "fitStats=" + fitStats +
                '}';
    }
}
