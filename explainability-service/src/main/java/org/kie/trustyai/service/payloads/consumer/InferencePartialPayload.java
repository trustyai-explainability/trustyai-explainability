package org.kie.trustyai.service.payloads.consumer;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class InferencePartialPayload implements PartialPayload {

    @JsonProperty("modelid")
    private String modelId;
    private String data;

    private PartialKind kind;

    @Id
    private String id;

    @ElementCollection
    private Map<String, String> metadata = new HashMap<>();

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

    public PartialKind getKind() {
        return this.kind;
    }

    public void setKind(PartialKind kind) {
        this.kind = kind;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
