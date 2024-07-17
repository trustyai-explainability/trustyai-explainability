package org.kie.trustyai.service.payloads.explainers.tssaliency;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for a time-series TSSaliency explanation.
 * Specific parameters for the TSSaliency explainer are defined in the {@link TSSaliencyParameters} class.
 */
public class TSSaliencyExplanationRequest {

    public List<String> getPredictionIds() {
        return predictionIds;
    }

    public void setPredictionIds(List<String> predictionIds) {
        this.predictionIds = predictionIds;
    }

    public TSSaliencyExplanationConfig getConfig() {
        return config;
    }

    public void setConfig(TSSaliencyExplanationConfig config) {
        this.config = config;
    }

    @JsonProperty(value = "predictionIds", required = true)
    private List<String> predictionIds;

    @JsonProperty(value = "config", required = true)
    private TSSaliencyExplanationConfig config = new TSSaliencyExplanationConfig();

}
