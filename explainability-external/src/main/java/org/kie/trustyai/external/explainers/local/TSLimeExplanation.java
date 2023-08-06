package org.kie.trustyai.external.explainers.local;

import java.util.Map;

public class TSLimeExplanation {

    private final Object historyWeights;
    private final Object yPerturbations;
    private final Object modelPrediction;
    private final Object xPerturbations;
    private final Object surrogatePrediction;

    public TSLimeExplanation(Map<String, Object> explanation) {
        this.historyWeights = explanation.get("history_weights");
        this.yPerturbations = explanation.get("current_forecast");
        this.modelPrediction = explanation.get("model_prediction");
        this.xPerturbations = explanation.get("x_perturbations");
        this.surrogatePrediction = explanation.get("surrogate_prediction");
    }

    public Object getHistoryWeights() {
        return historyWeights;
    }

    public Object getyPerturbations() {
        return yPerturbations;
    }

    public Object getModelPrediction() {
        return modelPrediction;
    }

    public Object getxPerturbations() {
        return xPerturbations;
    }

    public Object getSurrogatePrediction() {
        return surrogatePrediction;
    }
}
