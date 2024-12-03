package org.kie.trustyai.service.payloads.explainers.lime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configuration for the LIME explanation, including model and explainer parameters")
public class LimeExplanationConfig {

    @Schema(required = true, description = "Model configuration")
    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();

    @Schema(required = false, description = "Explainer configuration")
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
