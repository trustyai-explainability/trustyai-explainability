package org.kie.trustyai.service.payloads.consumer;

import java.util.Map;

public class KServeOutputPayload implements PartialPayload {

    private String id;
    private String modelId;
    private InferenceLoggerOutput data;

    public KServeOutputPayload() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public Map<String, String> getMetadata() {
        return null;
    }

    public InferenceLoggerOutput getData() {
        return data;
    }

    public void setData(InferenceLoggerOutput data) {
        this.data = data;
    }
}
