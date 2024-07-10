package org.kie.trustyai.service.payloads.explainers.shap;

import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SHAPExplanationConfig {

    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();

    @JsonProperty(value = "explainer")
    private SHAPExplainerConfig explainerConfig = new SHAPExplainerConfig();

    public SHAPExplainerConfig getExplainerConfig() {
        return this.explainerConfig;
    }

    public void setExplainerConfig(SHAPExplainerConfig explainerConfig) {
        this.explainerConfig = explainerConfig;
    }

    public ModelConfig getModelConfig() {
        return this.modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
}
