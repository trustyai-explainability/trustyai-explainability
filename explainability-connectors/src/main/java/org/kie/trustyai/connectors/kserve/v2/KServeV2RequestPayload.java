package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.KServeDatatype;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.PredictionInput;

public class KServeV2RequestPayload {

    public List<Inputs> inputs;

    public KServeV2RequestPayload() {
        // NO OP - Required for JSON deserialisation
    }

    public KServeV2RequestPayload(List<Inputs> inputs) {
        this.inputs = inputs;
    }

    public List<PredictionInput> toPredictionInputs() {

        final List<PredictionInput> predictionInputs = new ArrayList<>();
        final Inputs inputs = this.inputs.get(0);
        for (List<Object> values : inputs.data) {
            final int size = values.size();
            final List<Feature> features = IntStream.range(0, size)
                    .mapToObj(i -> KServeV2HTTPPayloadParser.getFeature(inputs.datatype, i, values.get(i)))
                    .collect(Collectors.toUnmodifiableList());
            predictionInputs.add(new PredictionInput(features));
        }
        return predictionInputs;
    }

    @Override
    public String toString() {
        return "KServeV1RequestPayload{" +
                "inputs=" + inputs +
                '}';
    }

    public static class Inputs {
        public String name;
        public List<List<Object>> data;
        public KServeDatatype datatype;
        public List<Integer> shape;
    }
}
