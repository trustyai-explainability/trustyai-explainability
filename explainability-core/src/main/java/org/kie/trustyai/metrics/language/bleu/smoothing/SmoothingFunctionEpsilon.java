package org.kie.trustyai.metrics.language.bleu.smoothing;

public class SmoothingFunctionEpsilon implements SmoothingFunction {

    private final double epsilon;
    private static final double DEFAULT_EPSILON = 0.1;

    public SmoothingFunctionEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * Initializes a new instance of the {@link SmoothingFunction} class with default parameters.
     */
    public SmoothingFunctionEpsilon() {
        this(DEFAULT_EPSILON);
    }

    /**
     * Add epsilon counts to precisions with zero counts.
     *
     * @param rawScores An array of precision scores for n-grams.
     * @return An array of smoothed precision scores.
     */
    public double[] apply(double[] rawScores) {
        double[] smoothed = new double[rawScores.length];
        for (int i = 0; i < rawScores.length; i++) {
            smoothed[i] = rawScores[i] > 0 ? rawScores[i] : epsilon;
        }
        return smoothed;
    }

}
