package org.kie.trustyai.service.payloads.data.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class ModelInferRequestPayload extends ModelInferBasePayload {
    @JsonProperty("inputs")
    private TensorPayload[] inputs;

    public TensorPayload[] getTensorPayloads() {
        return inputs;
    }

    public void setTensorPayloads(TensorPayload[] inputs) {
        this.inputs = inputs;
    }

    @Override
    public String toString() {
        return "ModelInferRequestPayload{" +
                "id='" + getId() + '\'' +
                ", inputs=" + Arrays.toString(inputs) +
                '}';
    }
}
