package org.kie.trustyai.service.data.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class CSVUtils {

    public static List<Class<?>> getSchema(CSVRecord sample) {
        return sample.stream().map(entry -> {
            if (NumberUtils.isParsable(entry)) {
                if (entry.contains(".")) {
                    return Double.class;
                } else {
                    return Integer.class;
                }
            } else {
                return String.class;
            }
        }).collect(Collectors.toList());
    }

    public static List<PredictionInput> parseInputs(String in) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new StringReader(in));
        final List<String> header = parser.getHeaderNames();
        final List<Class<?>> schema = new ArrayList<>();
        return parser.stream().map(entry -> {
            if (schema.isEmpty()) {
                schema.addAll(getSchema(entry));
            }
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < entry.size(); i++) {
                final String name = header.get(i);
                final Class<?> type = schema.get(i);
                if (type.equals(Double.class)) {
                    features.add(FeatureFactory.newNumericalFeature(name, Double.valueOf(entry.get(i))));
                } else if (type.equals(Integer.class)) {
                    features.add(FeatureFactory.newNumericalFeature(name, Integer.valueOf(entry.get(i))));
                } else {
                    features.add(FeatureFactory.newCategoricalFeature(name, entry.get(i)));
                }
            }
            return new PredictionInput(features);
        }).collect(Collectors.toList());
    }

    public static List<PredictionOutput> parseOutputs(String in) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new StringReader(in));
        final List<String> header = parser.getHeaderNames();
        final List<Class<?>> schema = new ArrayList<>();
        return parser.stream().map(entry -> {
            if (schema.isEmpty()) {
                schema.addAll(getSchema(entry));
            }
            final List<Output> outputs = new ArrayList<>();
            for (int i = 0; i < entry.size(); i++) {
                final String name = header.get(i);
                final Class<?> type = schema.get(i);
                if (type.equals(Double.class)) {
                    outputs.add(new Output(name, Type.NUMBER, new Value(Double.valueOf(entry.get(i))), 1.0));
                } else if (type.equals(Integer.class)) {
                    outputs.add(new Output(name, Type.NUMBER, new Value(Integer.valueOf(entry.get(i))), 1.0));
                } else {
                    outputs.add(new Output(name, Type.CATEGORICAL, new Value(entry.get(i)), 1.0));
                }
            }
            return new PredictionOutput(outputs);
        }).collect(Collectors.toList());
    }
}
