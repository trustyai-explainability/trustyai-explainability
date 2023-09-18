package org.kie.trustyai.metrics.drift.meanshift;

public class MeanshiftResult {
    private final double tStat;
    private final double pValue;
    private final boolean reject;

    public MeanshiftResult(double tStat, double pValue, boolean reject) {
        this.tStat = tStat;
        this.pValue = pValue;
        this.reject = reject;
    }

    public double gettStat() {
        return tStat;
    }

    public double getpValue() {
        return pValue;
    }

    public boolean isReject() {
        return reject;
    }

    @Override
    public String toString() {
        return "MeanshiftResult{" +
                "tStat=" + tStat +
                ", pValue=" + pValue +
                ", reject=" + reject +
                '}';
    }
}
