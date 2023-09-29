package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceLoggerInput {
    @JsonProperty("instances")
    private List<List<Double>> instances;

    public List<List<Double>> getInstances() {
        return instances;
    }

    public void setInstances(List<List<Double>> instances) {
        this.instances = instances;
    }
}
