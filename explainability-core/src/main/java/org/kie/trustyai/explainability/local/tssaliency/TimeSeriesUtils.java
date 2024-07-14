package org.kie.trustyai.explainability.local.tssaliency;

import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.*;

public class TimeSeriesUtils {

    public static List<Prediction> toTSSaliencyTimeSeries(List<Prediction> inputs) {
        final int nFeatures = inputs.size();
        final int timepoints = inputs.get(0).getInput().getFeatures().size();

        final List<Feature> features = new ArrayList<>();
        for (int t = 0; t < timepoints; t++) {
            double[] tFeatures = new double[nFeatures];
            for (int f = 0; f < nFeatures; f++) {
                tFeatures[f] = inputs.get(f).getInput().getFeatures().get(t).getValue().asNumber();
            }
            final Feature tFeature = FeatureFactory.newVectorFeature("t-" + t, tFeatures);
            features.add(tFeature);
        }

        final PredictionInput input = new PredictionInput(features);

        final List<Output> outputs = new ArrayList<>();
        for (int f = 0; f < nFeatures; f++) {
            outputs.add(inputs.get(f).getOutput().getOutputs().get(0));
        }

        final PredictionOutput output = new PredictionOutput(outputs);

        return List.of(new SimplePrediction(input, output));
    }

    public static List<PredictionInput> fromTSSaliencyTimeSeries(List<PredictionInput> inputs) {
        final int nFeatures = inputs.get(0).getFeatures().get(0).getValue().asVector().length;
        final int timepoints = inputs.get(0).getFeatures().size();

        final List<PredictionInput> predictions = new ArrayList<>();
        for (int f = 0; f < nFeatures; f++) {
            final List<Feature> features = new ArrayList<>();
            final double[] tValues = new double[timepoints];
            for (int t = 0; t < timepoints; t++) {
                tValues[t] = inputs.get(0).getFeatures().get(t).getValue().asVector()[0];
                features.add(FeatureFactory.newNumericalFeature("element-" + t, tValues[t]));
            }

            PredictionInput input = new PredictionInput(features);
            predictions.add(input);
        }

        return predictions;
    }

}
