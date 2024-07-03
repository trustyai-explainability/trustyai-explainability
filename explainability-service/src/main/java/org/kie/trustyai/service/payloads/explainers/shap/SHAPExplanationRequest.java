package org.kie.trustyai.service.payloads.explainers.shap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SHAPExplanationRequest {

    @JsonProperty(value = "predictionId", required = true)
    private String predictionId;

    @JsonProperty(value = "config", required = true)
    private SHAPExplanationConfig config = new SHAPExplanationConfig();

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public SHAPExplanationConfig getExplanationConfig() {
        return this.config;
    }

    public void setExplanationConfig(SHAPExplanationConfig explanationConfig) {
        this.config = explanationConfig;
    }

}
