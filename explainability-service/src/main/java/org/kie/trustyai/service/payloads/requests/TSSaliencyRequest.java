package org.kie.trustyai.service.payloads.requests;

import java.util.List;
import java.util.Map;

public class TSSaliencyRequest {
    private String modelId;

    private String predictionId;

    private Map<String, List<Double>> data;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

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
}
