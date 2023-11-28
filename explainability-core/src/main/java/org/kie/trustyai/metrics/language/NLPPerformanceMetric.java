package org.kie.trustyai.metrics.language;

/**
 *
 * @param <T>: the return type of the performance metric
 * @param <R>: The type of the performance metric reference object
 */
public interface NLPPerformanceMetric<T, R> {
    T calculate(R reference, String hypothesis);
}
