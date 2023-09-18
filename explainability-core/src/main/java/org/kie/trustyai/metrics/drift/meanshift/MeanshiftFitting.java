package org.kie.trustyai.metrics.drift.meanshift;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;

public class MeanshiftFitting {
    java.util.Map<String, StatisticalSummaryValues> fitStats;

    public MeanshiftFitting(Map<String, StatisticalSummaryValues> fitStats) {
        this.fitStats = fitStats;
    }

    public Map<String, StatisticalSummaryValues> getFitStats() {
        return fitStats;
    }

    @Override
    public String toString() {
        return "MeanshiftFitting{" +
                "fitStats=" + fitStats +
                '}';
    }
}
