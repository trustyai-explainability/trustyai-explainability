package org.kie.trustyai.service.payloads.explainers.tssaliency;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configuration for the TSSaliency explanation, including model and explainer parameters")
public class TSSaliencyExplanationConfig {

    @Schema(required = true, description = "Model configuration")
    @JsonProperty(value = "model", required = true)
    private ModelConfig modelConfig = new ModelConfig();

    @Schema(required = false, description = "Explainer configuration")
    @JsonProperty(value = "explainer")
    private TSSaliencyExplainerConfig explainerConfig = new TSSaliencyExplainerConfig();

    public TSSaliencyExplainerConfig getExplainerConfig() {
        return this.explainerConfig;
    }

    public void setExplainerConfig(TSSaliencyExplainerConfig explainerConfig) {
        this.explainerConfig = explainerConfig;
    }

    public ModelConfig getModelConfig() {
        return this.modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
}
