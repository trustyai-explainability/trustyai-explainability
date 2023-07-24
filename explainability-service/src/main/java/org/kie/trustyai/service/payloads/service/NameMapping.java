package org.kie.trustyai.service.payloads.service;

import java.util.Map;
import java.util.Objects;

public class NameMapping {
    private final String modelId;
    private final Map<String, String> inputMapping;
    private final Map<String, String> outputMapping;

    public NameMapping(String modelId, Map<String, String> inputMapping, Map<String, String> outputMapping) {
        this.modelId = modelId;
        this.inputMapping = inputMapping;
        this.outputMapping = outputMapping;
    }

    public String getModelId() {
        return modelId;
    }

    public Map<String, String> getInputMapping() {
        return inputMapping;
    }

    public Map<String, String> getOutputMapping() {
        return outputMapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NameMapping that = (NameMapping) o;
        return modelId.equals(that.modelId) && inputMapping.equals(that.inputMapping) && outputMapping.equals(that.outputMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, inputMapping, outputMapping);
    }
}
