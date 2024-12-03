package org.kie.trustyai.service.payloads.explainers.shap;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configuration for the SHAP explanation, including model and explainer parameters")
public class SHAPExplanationConfig {

    @Schema(required = true, description = "Model configuration")
    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();

    @Schema(required = false, description = "Explainer configuration")
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
