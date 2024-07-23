package org.kie.trustyai.service.payloads.consumer.partial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class KServeInputPayload extends PartialPayload {
    @Column(columnDefinition = "text")
    private String data;

    private String modelId;

    public KServeInputPayload() {
        metadata = null;
        setKind(PartialKind.request);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
