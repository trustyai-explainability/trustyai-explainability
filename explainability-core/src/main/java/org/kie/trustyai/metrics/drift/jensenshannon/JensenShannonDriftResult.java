package org.kie.trustyai.metrics.drift.jensenshannon;

public class JensenShannonDriftResult {
    private final double jsStat;
    private final double threshold;
    private final boolean reject;

    public JensenShannonDriftResult(double jsStat, double threshold, boolean reject) {
        this.jsStat = jsStat;
        this.threshold = threshold;
        this.reject = reject;
    }

    public double getjsStat() {
        return jsStat;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isReject() {
        return reject;
    }

    @Override
    public String toString() {
        return "JensenShannonDriftResult{" +
                "jsStat=" + jsStat +
                ", threshold=" + threshold +
                ", reject=" + reject +
                "}";
    }
}
