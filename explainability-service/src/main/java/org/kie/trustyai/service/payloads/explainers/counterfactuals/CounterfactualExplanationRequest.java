package org.kie.trustyai.service.payloads.explainers.counterfactuals;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CounterfactualExplanationRequest {

    @JsonProperty(value = "predictionId", required = true)
    private String predictionId;
    @JsonProperty(value = "config", required = true)
    private CounterfactualExplanationConfig config = new CounterfactualExplanationConfig();
    private Map<String, String> goals;

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public CounterfactualExplanationConfig getExplanationConfig() {
        return this.config;
    }

    public void setExplanationConfig(CounterfactualExplanationConfig explanationConfig) {
        this.config = explanationConfig;
    }

    public Map<String, String> getGoals() {
        return goals;
    }

    public void setGoals(Map<String, String> goals) {
        this.goals = goals;
    }

}
