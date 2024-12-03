package org.kie.trustyai.connectors.kserve.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

import static org.kie.trustyai.connectors.kserve.PayloadParser.DEFAULT_INPUT_PREFIX;

public class KServeV1RequestPayload {

    public KServeV1RequestPayload() {
        // NO OP - Required for JSON deserialisation
    }

    public List<List<Double>> instances;

    public KServeV1RequestPayload(List<List<Double>> instances) {
        this.instances = instances;
    }

    public List<PredictionInput> toPredictionInputs() {

        final List<PredictionInput> predictionInputs = new ArrayList<>();
        for (List<Double> instance : instances) {
            final int size = instance.size();
            final List<Feature> features = IntStream.range(0, size)
                    .mapToObj(i -> FeatureFactory.newNumericalFeature(DEFAULT_INPUT_PREFIX + "-" + i, instance.get(i)))
                    .collect(Collectors.toUnmodifiableList());
            predictionInputs.add(new PredictionInput(features));
        }
        return predictionInputs;
    }

    @Override
    public String toString() {
        return "KServeV1RequestPayload{" +
                "instances=" + instances +
                '}';
    }
}
