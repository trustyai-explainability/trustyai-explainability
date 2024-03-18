package org.kie.trustyai.metrics.language.utils;

public class F1Score {

    private F1Score() {

    }

    /**
     * Calculate F1 score given the precison and recall.
     *
     * @param precision
     * @param recall
     * @return F1 score
     */
    public static double calculate(double precision, double recall) {
        if (precision + recall > 0.0) {
            return 2 * precision * recall / (precision + recall);
        } else {
            return 0.0;
        }
    }
}
