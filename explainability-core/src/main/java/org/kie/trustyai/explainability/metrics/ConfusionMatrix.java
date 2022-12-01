package org.kie.trustyai.explainability.metrics;

public class ConfusionMatrix {

    private final int truePositives;
    private final int trueNegatives;
    private final int falsePositives;
    private final int falseNegatives;

    private ConfusionMatrix(int truePositives, int trueNegatives, int falsePositives, int falseNegatives) {
        this.truePositives = truePositives;
        this.trueNegatives = trueNegatives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
    }

    public static ConfusionMatrix create(int truePositives, int trueNegatives, int falsePositives, int falseNegatives) {
        return new ConfusionMatrix(truePositives, trueNegatives, falsePositives, falseNegatives);
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

}
