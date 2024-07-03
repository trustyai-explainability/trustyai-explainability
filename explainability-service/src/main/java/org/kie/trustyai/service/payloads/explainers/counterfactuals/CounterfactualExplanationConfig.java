package org.kie.trustyai.service.payloads.explainers.counterfactuals;

import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CounterfactualExplanationConfig {

    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();
    @JsonProperty(value = "explainer")
    private CounterfactualExplainerConfig explainerConfig = new CounterfactualExplainerConfig();

    public CounterfactualExplainerConfig getExplainerConfig() {
        return this.explainerConfig;
    }

    public void setExplainerConfig(CounterfactualExplainerConfig explainerConfig) {
        this.explainerConfig = explainerConfig;
    }

    public ModelConfig getModelConfig() {
        return this.modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
}
