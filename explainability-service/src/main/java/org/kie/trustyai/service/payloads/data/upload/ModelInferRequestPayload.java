package org.kie.trustyai.service.payloads.data.upload;

import java.util.Arrays;

public class ModelInferRequestPayload {
    private String id;
    private TensorPayload[] inputs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TensorPayload[] getInputs() {
        return inputs;
    }

    public void setInputs(TensorPayload[] inputs) {
        this.inputs = inputs;
    }

    @Override
    public String toString() {
        return "ModelJsonPayload{" +
                "id='" + id + '\'' +
                ", inputs=" + Arrays.toString(inputs) +
                '}';
    }
}
