package org.kie.trustyai.service.payloads.service;

import java.util.Map;
import java.util.Objects;

public class NameMapping {
    private final String modelID;
    private final Map<String, String> inputMapping;
    private final Map<String, String> outputMapping;

    public NameMapping(String modelID, Map<String, String> inputMapping, Map<String, String> outputMapping) {
        this.modelID = modelID;
        this.inputMapping = inputMapping;
        this.outputMapping = outputMapping;
    }

    public String getModelID() {
        return modelID;
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
        return modelID.equals(that.modelID) && inputMapping.equals(that.inputMapping) && outputMapping.equals(that.outputMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelID, inputMapping, outputMapping);
    }
}
