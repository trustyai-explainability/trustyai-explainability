package org.kie.trustyai.connectors.kserve.v2;

import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;

class KServePayloads {
    private final ModelInferRequest input;
    private final ModelInferResponse output;

    KServePayloads(ModelInferRequest input, ModelInferResponse output) {
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