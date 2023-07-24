package org.kie.trustyai.service.data.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

public class CSVUtils {

    private CSVUtils() {

    }

    public static List<Prediction> parse(String in, Metadata metadata) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(in));

        final List<String> inputNames = metadata.getInputSchema().retrieveNameMappedItems().entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().getIndex()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        final List<String> outputNames = metadata.getOutputSchema().retrieveNameMappedItems().entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().getIndex()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return parser.stream().map(entry -> {
            final List<Feature> inputFeatures = inputNames.stream().map(colName -> {
                final SchemaItem schemaItem = metadata.getInputSchema().retrieveNameMappedItems().get(colName);
                final int inputIndex = schemaItem.getIndex();
                final String name = schemaItem.getName();
                final String valueString = entry.get(inputIndex);
                final DataType types = schemaItem.getType();
                if (types.equals(DataType.BOOL)) {
                    return FeatureFactory.newBooleanFeature(name, Boolean.valueOf(valueString));
                } else if (types.equals(DataType.DOUBLE) || types.equals(DataType.FLOAT)) {
                    return FeatureFactory.newNumericalFeature(name, Double.valueOf(valueString));
                } else if (types.equals(DataType.INT32)) {
                    return FeatureFactory.newNumericalFeature(name, Integer.valueOf(valueString));
                } else if (types.equals(DataType.INT64)) {
                    return FeatureFactory.newNumericalFeature(name, Long.valueOf(valueString));
                } else {
                    return FeatureFactory.newCategoricalFeature(name, valueString);
                }
            }).collect(Collectors.toList());

            final List<Output> outputs = outputNames.stream().map(colName -> {
                final SchemaItem schemaItem = metadata.getOutputSchema().retrieveNameMappedItems().get(colName);
                final int inputIndex = schemaItem.getIndex();
                final String name = schemaItem.getName();
                final DataType vtypes = schemaItem.getType();
                final String valueString = entry.get(inputIndex);
                final Type types = PayloadConverter.convertToType(schemaItem.getType());
                if (types.equals(Type.BOOLEAN)) {
                    return new Output(name, Type.BOOLEAN, new Value(Boolean.valueOf(valueString)), 1.0);
                } else if (types.equals(Type.NUMBER)) {
                    if (vtypes.equals(DataType.DOUBLE) || vtypes.equals(DataType.FLOAT)) {
                        return new Output(name, Type.NUMBER, new Value(Double.valueOf(valueString)), 1.0);
                    } else if (vtypes.equals(DataType.INT32)) {
                        return new Output(name, Type.NUMBER, new Value(Integer.valueOf(valueString)), 1.0);
                    } else {
                        return new Output(name, Type.NUMBER, new Value(Long.valueOf(valueString)), 1.0);
                    }
                } else {
                    return new Output(name, Type.CATEGORICAL, new Value(valueString), 1.0);
                }
            }).collect(Collectors.toList());

            final PredictionInput predictionInput = new PredictionInput(inputFeatures);
            final PredictionOutput predictionOutput = new PredictionOutput(outputs);

            return new SimplePrediction(predictionInput, predictionOutput);
        }).collect(Collectors.toList());
    }

    public static List<List<Value>> parseRaw(String in) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(in));
        return parser.stream().map(entry -> entry.stream().map(Value::new).collect(Collectors.toList())).collect(Collectors.toList());
    }
}
