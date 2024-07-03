package org.kie.trustyai.service.payloads.explainers.lime;

import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LimeExplanationConfig {

    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();

    @JsonProperty(value = "explainer")
    private LimeExplainerConfig explainerConfig = new LimeExplainerConfig();

    public LimeExplainerConfig getExplainerConfig() {
        return this.explainerConfig;
    }

    public void setExplainerConfig(LimeExplainerConfig explainerConfig) {
        this.explainerConfig = explainerConfig;
    }

    public ModelConfig getModelConfig() {
        return this.modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
}
