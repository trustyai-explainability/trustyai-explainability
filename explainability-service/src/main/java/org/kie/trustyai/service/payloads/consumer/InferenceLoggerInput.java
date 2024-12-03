package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceLoggerInput {
    @JsonProperty("instances")
    private List<List<Object>> instances;

    public List<List<Object>> getInstances() {
        return instances;
    }

    public void setInstances(List<List<Object>> instances) {
        this.instances = instances;
    }
}
