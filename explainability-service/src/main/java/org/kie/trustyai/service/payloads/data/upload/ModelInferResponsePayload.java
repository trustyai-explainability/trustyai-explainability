package org.kie.trustyai.service.payloads.data.upload;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelInferResponsePayload {
    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    private String id;
    private Map<String, Object> parameters;
    private TensorPayload[] outputs;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public TensorPayload[] getOutputs() {
        return outputs;
    }

    public void setOutputs(TensorPayload[] outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return "ModelInferResponsePayload{" +
                "modelName='" + modelName + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                ", id='" + id + '\'' +
                ", parameters=" + parameters +
                ", outputs=" + Arrays.toString(outputs) +
                '}';
    }
}
