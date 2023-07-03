package org.kie.trustyai.service.utils;

import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;

public class KServePayloads {
    private final ModelInferRequest input;
    private final ModelInferResponse output;

    public KServePayloads(ModelInferRequest input, ModelInferResponse output) {
        this.input = input;
        this.output = output;
    }

    public ModelInferRequest getInput() {
        return input;
    }

    public ModelInferResponse getOutput() {
        return output;
    }
}