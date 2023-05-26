package org.kie.trustyai.service.payloads.consumer;

import java.util.HashMap;
import java.util.Map;

public class InferencePayload {

    private String input;
    private String output;
    private String modelId;

    private Map<String, String> metadata = new HashMap<>();

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
