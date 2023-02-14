package org.kie.trustyai.service.payloads;

public class MetricThreshold {
    public double lowerBound;
    public double upperBound;
    public boolean outsideBounds;

    public MetricThreshold(double lowerBound, double upperBound, double value) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.outsideBounds = !(value <= upperBound) || !(value >= lowerBound);
    }
}
