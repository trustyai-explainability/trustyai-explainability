package org.kie.trustyai.metrics.language.distance;

public class LevenshteinResult {
    private final int distance;
    private final LevenshteinCounters counters;

    public LevenshteinResult(int distance, LevenshteinCounters counters) {
        this.distance = distance;
        this.counters = counters;
    }

    public int getDistance() {
        return distance;
    }

    public LevenshteinCounters getCounters() {
        return counters;
    }
}
