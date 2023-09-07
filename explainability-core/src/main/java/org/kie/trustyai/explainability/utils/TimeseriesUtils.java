package org.kie.trustyai.explainability.utils;

import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

public class TimeseriesUtils {
    /**
     * Convert predictions containing a single vector feature to a list of features.
     *
     * @param inputs
     * @return A list of {@link PredictionInput}
     */
    public static List<PredictionInput> featureVectorTofeatureList(List<PredictionInput> inputs) {
        final List<PredictionInput> transformedInputs = new ArrayList<>();
        for (final PredictionInput input : inputs) {
            final double[] vector = input.getFeatures().get(0).getValue().asVector();
            final List<Feature> features = new ArrayList<>();
            for (int n = 0; n < vector.length; n++) {
                features.add(FeatureFactory.newNumericalFeature("x-" + n, vector[n]));
            }
            transformedInputs.add(new PredictionInput(features));
        }
        return transformedInputs;
    }

    /**
     * Convert predictions containing a list of features to a single vector feature.
     *
     * @param inputs
     * @return A list of {@link PredictionInput}
     */
    public static List<PredictionInput> featureListTofeatureVector(List<PredictionInput> inputs) {
        final List<PredictionInput> transformedInputs = new ArrayList<>();
        for (final PredictionInput input : inputs) {
            transformedInputs.add(featureListTofeatureVector(input));
        }
        return transformedInputs;
    }

    /**
     * Convert a single prediction containing a list of features to a single vector feature.
     *
     * @param input
     * @return
     */
    public static PredictionInput featureListTofeatureVector(PredictionInput input) {
        final double[] vector = input.getFeatures().stream().mapToDouble(f -> f.getValue().asNumber()).toArray();
        return new PredictionInput(List.of(FeatureFactory.newVectorFeature("x", vector)));
    }

}
