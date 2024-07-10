package org.kie.trustyai.metrics.drift.utils;

public class KLDivergence {
    private final double[] p1;
    private final double[] p2;

    public KLDivergence(double[] p1, double[] p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Calculates the Kullback-Leibler divergence between arrays.
     *
     * @param p1 The reference array.
     * @param p2 The hypothesis array.
     * @return The Kullback-Leibler divergence.
     */
    public double calculate() {
        double divergence = 0.0;
        for (int i = 0; i < this.p1.length; i++) {
            if (this.p1[i] != 0) {
                divergence += this.p1[i] * Math.log(this.p1[i] / this.p2[i]);
            }
            if (this.p2[i] == 0) {
                divergence += this.p1[i];
            }
        }
        return divergence;
    }
}
