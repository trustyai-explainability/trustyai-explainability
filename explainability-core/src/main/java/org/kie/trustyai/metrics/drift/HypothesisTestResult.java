package org.kie.trustyai.metrics.drift;

public class HypothesisTestResult {

    private final double statVal;
    private final double pValue;
    private final boolean reject;

    public HypothesisTestResult(double statVal, double pValue, boolean reject) {
        this.statVal = statVal;
        this.pValue = pValue;
        this.reject = reject;
    }

    public double getpValue() {
        return pValue;
    }

    public boolean isReject() {
        return reject;
    }

    public double getStatVal() {
        return statVal;
    }

}