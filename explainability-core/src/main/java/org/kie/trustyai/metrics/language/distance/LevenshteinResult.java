package org.kie.trustyai.metrics.language.distance;

import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;

public class LevenshteinResult {
    private final int distance;
    private final LevenshteinCounters counters;
    private final RealMatrix distanceMatrix;
    private final List<String> referenceTokens;
    private final List<String> hypothesisTokens;

    public LevenshteinResult(int distance, LevenshteinCounters counters, RealMatrix distanceMatrix, List<String> referenceTokens, List<String> hypothesisTokens) {
        this.distance = distance;
        this.counters = counters;
        this.distanceMatrix = distanceMatrix;
        this.referenceTokens = referenceTokens;
        this.hypothesisTokens = hypothesisTokens;
    }

    public int getDistance() {
        return distance;
    }

    public LevenshteinCounters getCounters() {
        return counters;
    }

    public RealMatrix getDistanceMatrix() {
        return distanceMatrix;
    }

    public List<String> getReferenceTokens() {
        return referenceTokens;
    }

    public List<String> getHypothesisTokens() {
        return hypothesisTokens;
    }
}
