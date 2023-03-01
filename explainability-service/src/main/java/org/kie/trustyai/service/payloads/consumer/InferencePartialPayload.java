package org.kie.trustyai.service.payloads.consumer;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferencePartialPayload {

    @JsonProperty("modelid")
    private String modelId;
    private String data;

    private String kind;

    @JsonProperty("uuid")
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
