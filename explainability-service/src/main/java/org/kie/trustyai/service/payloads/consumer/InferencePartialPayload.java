package org.kie.trustyai.service.payloads.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferencePartialPayload {

    @JsonProperty("modelid")
    private String modelId;
    private String data;

    private String kind;

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getKind() {
        return this.kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
