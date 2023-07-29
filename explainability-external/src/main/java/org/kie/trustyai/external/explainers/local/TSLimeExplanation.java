package org.kie.trustyai.external.explainers.local;

import java.util.Map;

public class TSLimeExplanation {

    private final double[] historyWeights;
    private final double[] yPerturbations;
    private final double[] modelPrediction;
    private final double[] xPerturbations;
    private final double[] surrogatePrediction;

    public TSLimeExplanation(Map<String, Object> explanation) {
        this.historyWeights = (double[]) explanation.get("history_weights");
        this.yPerturbations = (double[]) explanation.get("current_forecast");
        this.modelPrediction = (double[]) explanation.get("model_prediction");
        this.xPerturbations = (double[]) explanation.get("x_perturbations");
        this.surrogatePrediction = (double[]) explanation.get("surrogate_prediction");
    }

    public double[] getHistoryWeights() {
        return historyWeights;
    }

    public double[] getyPerturbations() {
        return yPerturbations;
    }

    public double[] getModelPrediction() {
        return modelPrediction;
    }

    public double[] getxPerturbations() {
        return xPerturbations;
    }

    public double[] getSurrogatePrediction() {
        return surrogatePrediction;
    }
}
