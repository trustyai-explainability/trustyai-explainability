package org.kie.trustyai.connectors.kserve.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

public class KServeV1RequestPayload {

    public KServeV1RequestPayload() {
        // NO OP - Required for JSON deserialisation
    }

    public List<List<Double>> instances;

    public KServeV1RequestPayload(List<List<Double>> instances) {
        this.instances = instances;
    }

    public List<PredictionInput> toPredictionInputs() {
        return instances.stream().map(values -> values.stream().map(value -> FeatureFactory.newNumericalFeature("f", value))
                .collect(Collectors.toList())).map(PredictionInput::new).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "KServeV1RequestPayload{" +
                "instances=" + instances +
                '}';
    }
}
