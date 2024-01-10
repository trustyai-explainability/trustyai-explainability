package org.kie.trustyai.metrics.language.levenshtein;

import org.kie.trustyai.metrics.language.distance.LevenshteinCounters;

public class ErrorRateResult {
    private final double value;
    private final LevenshteinCounters alignmentCounters;

    public ErrorRateResult(double value, LevenshteinCounters alignmentCounters) {
        this.value = value;
        this.alignmentCounters = alignmentCounters;
    }

    public double getValue() {
        return value;
    }

    public LevenshteinCounters getAlignmentCounters() {
        return alignmentCounters;
    }

    @Override
    public String toString() {
        return "Error Rate: " +
                value + System.lineSeparator() +
                System.lineSeparator() +
                alignmentCounters;
    }
}
