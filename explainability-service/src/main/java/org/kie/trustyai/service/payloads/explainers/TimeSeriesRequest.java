package org.kie.trustyai.service.payloads.explainers;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic request for a time-series explanation.
 * It contains the model configuration and either:
 * - The data to be explained or,
 * - A predictionId to be used to retrieve the data from storage.
 */
public class TimeSeriesRequest {
    @JsonProperty("model")
    private ModelConfig modelConfig;

    private String predictionId;

    private Map<String, List<Double>> data;

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public Map<String, List<Double>> getData() {
        return data;
    }

    public void setData(Map<String, List<Double>> data) {
        this.data = data;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
}
