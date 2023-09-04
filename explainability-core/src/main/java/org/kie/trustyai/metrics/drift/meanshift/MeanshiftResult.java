package org.kie.trustyai.metrics.drift.meanshift;

public class MeanshiftResult {
    double tstat;
    double pvalue;
    boolean reject;

    public MeanshiftResult(double tstat, double pvalue, boolean reject) {
        this.tstat = tstat;
        this.pvalue = pvalue;
        this.reject = reject;
    }
}
