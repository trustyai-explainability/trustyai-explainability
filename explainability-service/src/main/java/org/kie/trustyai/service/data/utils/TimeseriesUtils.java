package org.kie.trustyai.service.data.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

public class TimeseriesUtils {

    public static List<PredictionInput> transformData(Map<String, List<Double>> data, List<Object> timestamps, String timestampName) {

        final List<PredictionInput> result = new ArrayList<>();

        // TODO: Check sizes conform
        for (int i = 0; i < timestamps.size(); i++) {
            List<Feature> row = new ArrayList<>();
            row.add(FeatureFactory.newTextFeature(timestampName, timestamps.get(i).toString()));

            for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
                row.add(FeatureFactory.newNumericalFeature(entry.getKey(), entry.getValue().get(i)));
            }
            result.add(new PredictionInput(row));
        }
        return result;
    }
}
