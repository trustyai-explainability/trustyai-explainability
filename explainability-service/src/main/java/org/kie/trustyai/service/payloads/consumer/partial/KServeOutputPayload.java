package org.kie.trustyai.service.payloads.consumer.partial;

import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class KServeOutputPayload extends PartialPayload {
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "output_data", referencedColumnName = "id")
    private InferenceLoggerOutput data;

    private String modelId;

    public KServeOutputPayload() {
        metadata = null;
        setKind(PartialKind.response);
    }

    public InferenceLoggerOutput getData() {
        return data;
    }

    public void setData(InferenceLoggerOutput data) {
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
