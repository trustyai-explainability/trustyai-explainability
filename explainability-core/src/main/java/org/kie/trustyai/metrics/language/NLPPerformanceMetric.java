package org.kie.trustyai.metrics.language;

public interface NLPPerformanceMetric {
    double calculate(String reference, String hypothesis);
}
