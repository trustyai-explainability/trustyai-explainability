package org.kie.trustyai.metrics.language.bleu.smoothing;

public interface SmoothingFunction {
    double[] apply(double[] rawScores);
}
