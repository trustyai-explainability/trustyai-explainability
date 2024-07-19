package org.kie.trustyai.service.payloads.consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;

import java.util.Map;

@Entity
public class KServeInputPayload implements PartialPayload {

    @Id
    private String id;
    private String modelId;

    private String data;

    public KServeInputPayload() {
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
