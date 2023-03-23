package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;

import static org.kie.trustyai.explainability.model.Type.*;

public class TensorConverter {

    public static KServeDatatype trustyToKserveType(Type type, Object object) throws IllegalArgumentException {
        if (type == NUMBER) {
            if (object instanceof Integer) {
                return KServeDatatype.INT32;
            } else {
                return KServeDatatype.FP64;
            }
        } else if (type == BOOLEAN) {
            return KServeDatatype.BOOL;
        } else if (type == CATEGORICAL) {
            return KServeDatatype.BYTES;
        } else {
            throw new IllegalArgumentException("Unsupported TrustyAI type: " + type);
        }
    }

    public static List<Feature> inputTensorToFeatures(List<ModelInferRequest.InferInputTensor> tensors) {
        if (tensors.size() == 1) { // NP codec
            final ModelInferRequest.InferInputTensor tensor = tensors.get(0);
            return inputTensorToFeatures(tensor, null);
        } else { // PD codec
            return tensors.stream().map(tensor -> {
                final InferTensorContents responseInputContents = tensor.getContents();
                final KServeDatatype type;
                try {
                    type = KServeDatatype.valueOf(tensor.getDatatype());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
                }
                switch (type) {
                    case BOOL:
                        return FeatureFactory.newBooleanFeature(tensor.getName(), responseInputContents.getBoolContents(0));
                    case INT8:
                    case INT16:
                    case INT32:
                        return FeatureFactory.newNumericalFeature(tensor.getName(), responseInputContents.getIntContents(0));
                    case INT64:
                        return FeatureFactory.newNumericalFeature(tensor.getName(), responseInputContents.getInt64Contents(0));
                    case FP32:
                        return FeatureFactory.newNumericalFeature(tensor.getName(), responseInputContents.getFp32Contents(0));
                    case FP64:
                        return FeatureFactory.newNumericalFeature(tensor.getName(), responseInputContents.getFp64Contents(0));
                    case BYTES:
                        return FeatureFactory.newCategoricalFeature(tensor.getName(), String.valueOf(responseInputContents.getBytesContents(0)));
                    default:
                        throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
                }
            }).collect(Collectors.toList());
        }
    }

    public static List<Feature> inputTensorToFeatures(ModelInferRequest.InferInputTensor tensor,
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
                return inputFromContentList(responseInputContents.getBoolContentsList(), BOOLEAN, inputNames);
            case INT8:
            case INT16:
            case INT32:
                return inputFromContentList(responseInputContents.getIntContentsList(), NUMBER, inputNames);
            case INT64:
                return inputFromContentList(responseInputContents.getInt64ContentsList(), NUMBER, inputNames);
            case FP32:
                return inputFromContentList(responseInputContents.getFp32ContentsList(), NUMBER, inputNames);
            case FP64:
                return inputFromContentList(responseInputContents.getFp64ContentsList(), NUMBER, inputNames);
            case BYTES:
                return inputFromContentList(responseInputContents.getBytesContentsList(), CATEGORICAL, inputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    public static List<Output> outputTensorToOutputs(List<ModelInferResponse.InferOutputTensor> tensors) {
        if (tensors.size() == 1) { // NP codec
            final ModelInferResponse.InferOutputTensor tensor = tensors.get(0);
            return outputTensorToOutputs(tensor, null);
        } else { // PD codec
            return tensors.stream().map(tensor -> {
                final InferTensorContents contents = tensor.getContents();
                final KServeDatatype type;
                try {
                    type = KServeDatatype.valueOf(tensor.getDatatype());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
                }
                switch (type) {
                    case BOOL:
                        return new Output(tensor.getName(), Type.BOOLEAN, new Value(contents.getBoolContents(0)), 1.0);
                    case INT8:
                    case INT16:
                    case INT32:
                        return new Output(tensor.getName(), NUMBER, new Value(contents.getIntContents(0)), 1.0);
                    case INT64:
                        return new Output(tensor.getName(), NUMBER, new Value(contents.getInt64Contents(0)), 1.0);
                    case FP32:
                        return new Output(tensor.getName(), NUMBER, new Value(contents.getFp32Contents(0)), 1.0);
                    case FP64:
                        return new Output(tensor.getName(), NUMBER, new Value(contents.getFp64Contents(0)), 1.0);
                    case BYTES:
                        return new Output(tensor.getName(), CATEGORICAL, new Value(contents.getBytesContents(0)), 1.0);
                    default:
                        throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
                }
            }).collect(Collectors.toList());
        }
    }


    public static List<Output> outputTensorToOutputs(ModelInferResponse.InferOutputTensor tensor,
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
                return outputFromContentList(responseOutputContents.getBoolContentsList(), BOOLEAN, outputNames);
            case INT8:
            case INT16:
            case INT32:
                return outputFromContentList(responseOutputContents.getIntContentsList(), NUMBER, outputNames);
            case INT64:
                return outputFromContentList(responseOutputContents.getInt64ContentsList(), NUMBER, outputNames);
            case FP32:
                return outputFromContentList(responseOutputContents.getFp32ContentsList(), NUMBER, outputNames);
            case FP64:
                return outputFromContentList(responseOutputContents.getFp64ContentsList(), NUMBER, outputNames);
            case BYTES:
                return outputFromContentList(responseOutputContents.getBytesContentsList(), CATEGORICAL, outputNames);

            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }

    public static List<Output> outputFromContentList(List<?> values, Type type, List<String> outputNames) {
        final int size = values.size();
        List<String> names = outputNames == null ? IntStream.range(0, size).mapToObj(i -> "output-" + i).collect(Collectors.toList()) : outputNames;
        if (names.size() != size) {
            throw new IllegalArgumentException("Output names list has an incorrect size (" + names.size() + ", when it should be " + size + ")");
        }
        return IntStream.range(0, size)
                .mapToObj(i -> new Output(names.get(i), type, new Value(values.get(i)), 1.0))
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Feature> inputFromContentList(List<?> values, Type type, List<String> outputNames) {
        final int size = values.size();
        List<String> names = outputNames == null ? IntStream.range(0, size).mapToObj(i -> "input-" + i).collect(Collectors.toList()) : outputNames;
        if (names.size() != size) {
            throw new IllegalArgumentException("Input names list has an incorrect size (" + names.size() + ", when it should be " + size + ")");
        }
        return IntStream
                .range(0, size)
                .mapToObj(i -> new Feature(names.get(i), type, new Value(values.get(i))))
                .collect(Collectors.toUnmodifiableList());
    }

}
