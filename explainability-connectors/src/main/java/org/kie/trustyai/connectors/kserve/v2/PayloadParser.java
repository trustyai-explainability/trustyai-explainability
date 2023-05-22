package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;

import com.google.protobuf.ByteString;

public class PayloadParser {

    public static PredictionInput requestToInput(ModelInferRequest request, List<String> inputNames) throws IllegalArgumentException {

        // If we don't have raw contents, process with the default parser
        if (request.getRawInputContentsList().isEmpty()) {
            return inputTensorToPredictionInput(request.getInputs(0), inputNames);
        } else { // We have raw contents and need to parse accordingly
            return rawContentToPredictionInput(request, inputNames);
        }
    }

    public static PredictionInput rawContentToPredictionInput(ModelInferRequest request, List<String> inputNames) throws IllegalArgumentException {
        final ModelInferRequest.InferInputTensor tensor = request.getInputs(0);
        final ByteString raw = request.getRawInputContents(0);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        switch (type) {
            case BOOL:
                return inputFromContentList(RawConverter.toBoolean(raw), Type.BOOLEAN, inputNames);
            case INT8:
            case INT16:
            case INT32:
                return inputFromContentList(RawConverter.toInteger(raw), Type.NUMBER, inputNames);
            case INT64:
                return inputFromContentList(RawConverter.toLong(raw), Type.NUMBER, inputNames);
            case FP32:
                return inputFromContentList(RawConverter.toFloat(raw), Type.NUMBER, inputNames);
            case FP64:
                return inputFromContentList(RawConverter.toDouble(raw), Type.NUMBER, inputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }

    }

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

    public static PredictionOutput responseToOutput(ModelInferResponse response, List<String> outputNames) throws IllegalArgumentException {

        // If we don't have raw contents, process with the default parser
        if (response.getRawOutputContentsList().isEmpty()) {
            return outputTensorToPredictionOutput(response.getOutputs(0), outputNames);
        } else { // We have raw contents and need to parse accordingly
            return rawContentToPredictionOutput(response, outputNames);
        }
    }

    public static PredictionOutput rawContentToPredictionOutput(ModelInferResponse response,
            List<String> outputNames) throws IllegalArgumentException {
        final ModelInferResponse.InferOutputTensor tensor = response.getOutputs(0);
        final ByteString raw = response.getRawOutputContents(0);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }

        switch (type) {
            case BOOL:
                return outputFromContentList(RawConverter.toBoolean(raw), Type.BOOLEAN, outputNames);
            case INT8:
            case INT16:
            case INT32:
                return outputFromContentList(RawConverter.toInteger(raw), Type.NUMBER, outputNames);
            case INT64:
                return outputFromContentList(RawConverter.toLong(raw), Type.NUMBER, outputNames);
            case FP32:
                return outputFromContentList(RawConverter.toFloat(raw), Type.NUMBER, outputNames);
            case FP64:
                return outputFromContentList(RawConverter.toDouble(raw), Type.NUMBER, outputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
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
