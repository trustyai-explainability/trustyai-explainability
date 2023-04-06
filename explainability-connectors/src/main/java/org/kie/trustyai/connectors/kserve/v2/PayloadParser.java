package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;

public class PayloadParser {

    public static PredictionInput inputTensorToPredictionInput(ModelInferRequest.InferInputTensor tensor,
            List<String> inputNames) throws IllegalArgumentException {
        final InferTensorContents responseInputContents = tensor.getContents();
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        switch (type) {
            case BOOL:
                return inputFromContentList(responseInputContents.getBoolContentsList(), Type.BOOLEAN, inputNames);
            case INT8:
            case INT16:
            case INT32:
                return inputFromContentList(responseInputContents.getIntContentsList(), Type.NUMBER, inputNames);
            case INT64:
                return inputFromContentList(responseInputContents.getInt64ContentsList(), Type.NUMBER, inputNames);
            case FP32:
                return inputFromContentList(responseInputContents.getFp32ContentsList(), Type.NUMBER, inputNames);
            case FP64:
                return inputFromContentList(responseInputContents.getFp64ContentsList(), Type.NUMBER, inputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    public static PredictionOutput outputTensorToPredictionOutput(ModelInferResponse.InferOutputTensor tensor,
            List<String> outputNames) throws IllegalArgumentException {
        final InferTensorContents responseOutputContents = tensor.getContents();
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }

        switch (type) {
            case BOOL:
                return outputFromContentList(responseOutputContents.getBoolContentsList(), Type.BOOLEAN, outputNames);
            case INT8:
            case INT16:
            case INT32:
                return outputFromContentList(responseOutputContents.getIntContentsList(), Type.NUMBER, outputNames);
            case INT64:
                return outputFromContentList(responseOutputContents.getInt64ContentsList(), Type.NUMBER, outputNames);
            case FP32:
                return outputFromContentList(responseOutputContents.getFp32ContentsList(), Type.NUMBER, outputNames);
            case FP64:
                return outputFromContentList(responseOutputContents.getFp64ContentsList(), Type.NUMBER, outputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }

    public static PredictionOutput outputFromContentList(List<?> values, Type type, List<String> outputNames) {
        final int size = values.size();
        List<String> names = outputNames == null ? IntStream.range(0, size).mapToObj(i -> "output-" + i).collect(Collectors.toList()) : outputNames;
        if (names.size() != size) {
            throw new IllegalArgumentException("Output names list has an incorrect size (" + names.size() + ", when it should be " + size + ")");
        }
        return new PredictionOutput(IntStream
                .range(0, size)
                .mapToObj(i -> new Output(names.get(i), type, new Value(values.get(i)), 1.0))
                .collect(Collectors.toUnmodifiableList()));
    }

    public static PredictionInput inputFromContentList(List<?> values, Type type, List<String> outputNames) {
        final int size = values.size();
        List<String> names = outputNames == null ? IntStream.range(0, size).mapToObj(i -> "input-" + i).collect(Collectors.toList()) : outputNames;
        if (names.size() != size) {
            throw new IllegalArgumentException("Input names list has an incorrect size (" + names.size() + ", when it should be " + size + ")");
        }
        return new PredictionInput(IntStream
                .range(0, size)
                .mapToObj(i -> new Feature(names.get(i), type, new Value(values.get(i))))
                .collect(Collectors.toUnmodifiableList()));
    }

    public static void addFeature(InferTensorContents.Builder content, Feature feature) {
        final Object object = feature.getValue().getUnderlyingObject();
        final Type type = feature.getType();

        switch (type) {
            case NUMBER:
                if (object instanceof Integer) {
                    content.addIntContents((Integer) object);
                } else if (object instanceof Double) {
                    content.addFp64Contents((Double) object);
                }
                break;
            case BOOLEAN:
                content.addBoolContents((Boolean) object);
                break;
            default:
                throw new IllegalArgumentException("Unsupported feature type: " + type);
        }
    }

    public static InferTensorContents.Builder predictionInputToTensorContents(List<PredictionInput> inputs) {
        final InferTensorContents.Builder contents = InferTensorContents.newBuilder();

        inputs.stream()
                .map(PredictionInput::getFeatures)
                .forEach(features -> features.forEach(feature -> addFeature(contents, feature)));

        return contents;
    }
}
