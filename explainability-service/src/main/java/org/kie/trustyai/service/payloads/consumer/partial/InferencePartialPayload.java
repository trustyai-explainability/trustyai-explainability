package org.kie.trustyai.service.payloads.consumer.partial;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class InferencePartialPayload extends PartialPayload {
    // mariadb specific?
    @Lob
    @Column(length = 32_000_000)
    private String data;

    @JsonProperty("modelid")
    private String modelId;

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
