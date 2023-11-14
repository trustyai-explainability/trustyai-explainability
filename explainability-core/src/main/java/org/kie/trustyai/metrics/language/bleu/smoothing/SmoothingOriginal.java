package org.kie.trustyai.metrics.language.bleu.smoothing;

public class SmoothingOriginal implements SmoothingFunction {
    /**
     * No smoothing, returns the original precision scores.
     *
     * @param rawScores An array of precision scores for n-grams.
     * @return An array of smoothed precision scores.
     */
    public double[] apply(double[] rawScores) {
        return rawScores.clone();
    }
}
