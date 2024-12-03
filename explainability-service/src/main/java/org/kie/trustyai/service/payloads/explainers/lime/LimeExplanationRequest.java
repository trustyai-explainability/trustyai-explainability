package org.kie.trustyai.service.payloads.explainers.lime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LimeExplanationRequest {

    @JsonProperty(value = "predictionId", required = true)
    private String predictionId;

    @JsonProperty(value = "config", required = true)
    private LimeExplanationConfig config = new LimeExplanationConfig();

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public LimeExplanationConfig getConfig() {
        return this.config;
    }

    public void setConfig(LimeExplanationConfig explanationConfig) {
        this.config = explanationConfig;
    }

}
