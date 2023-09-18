package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceLoggerOutput {
    @JsonProperty("predictions")
    private List<Double> predictions;

    public List<Double> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Double> predictions) {
        this.predictions = predictions;
    }
}
