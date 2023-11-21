package org.kie.trustyai.metrics.language;

public interface NLPPerformanceMetric<T, R> {
    T calculate(R reference, String hypothesis);
}
