package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.Map;

public class FourierMMDFitting {
    java.util.Map<String, Object> fitStats;

    public FourierMMDFitting(Map<String, Object> fitStats) {
        this.fitStats = fitStats;
    }

    public Map<String, Object> getFitStats() {
        return fitStats;
    }

    @Override
    public String toString() {
        return "FourierMMDFitting{" +
                "fitStats=" + fitStats +
                '}';
    }
}
