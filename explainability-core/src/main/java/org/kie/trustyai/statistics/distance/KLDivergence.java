package org.kie.trustyai.statistics.distance;

public class KLDivergence {
    private final Double[] p1;
    private final Double[] p2;

    public KLDivergence(Double[] p1, Double[] p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Calculates the Kullback-Leibler divergence between arrays.
     *
     * @return The Kullback-Leibler divergence.
     */
    public double calculate() {
        double divergence = 0.0;
        for (int i = 0; i < this.p1.length; i++) {

            // check that this is a valid probability distribution
            if (this.p1[i] > 1 || this.p1[i] < 0) {
                throw new IllegalArgumentException(
                        String.format(
                                "Kullback-Leiber divergence requires both arrays to only contain values in the range [0,1], but p1[%d]=%f",
                                i, p1[i]));
            }

            if (this.p2[i] > 1 || this.p2[i] < 0) {
                throw new IllegalArgumentException(
                        String.format(
                                "Kullback-Leiber divergence requires both arrays to only contain values in the range [0,1], but p2[%d]=%f",
                                i, p2[i]));
            }

            if (this.p1[i] != 0 && this.p2[i] != 0) {
                divergence += this.p1[i] * Math.log(this.p1[i] / this.p2[i]);
            }
            if (this.p2[i] == 0) {
                divergence += this.p1[i];
            }
        }
        return divergence;
    }

}
