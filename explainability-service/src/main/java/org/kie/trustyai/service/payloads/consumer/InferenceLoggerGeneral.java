package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceLoggerGeneral {

    @JsonProperty("inputs")
    private List<InferenceLoggerInputV2> inputs;

    public InferenceLoggerGeneral(List<InferenceLoggerInputV2> inputs) {
        this.inputs = inputs;
    }

    public List<InferenceLoggerInputV2> getInputs() {
        return inputs;
    }

    public void setInputs(List<InferenceLoggerInputV2> inputs) {
        this.inputs = inputs;
    }

    public InferenceLoggerGeneral() {
    }
}
