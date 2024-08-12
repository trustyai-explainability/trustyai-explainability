package org.kie.trustyai.connectors.kserve.v2;

import org.apache.commons.lang3.NotImplementedException;
import org.kie.trustyai.connectors.kserve.KServeDatatype;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.tensor.Tensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.kie.trustyai.explainability.model.Type.BOOLEAN;
import static org.kie.trustyai.explainability.model.Type.CATEGORICAL;
import static org.kie.trustyai.explainability.model.Type.NUMBER;

public class TensorConverterUtils {

    // === LABELING AND TYPING UTILITIES ===============================================================================
    public static KServeDatatype inferKServeType(Object object) {
        if (object instanceof Integer) {
            return KServeDatatype.INT32;
        } else if (object instanceof Long) {
            return KServeDatatype.INT64;
        } else if (object instanceof Float) {
            return KServeDatatype.FP32;
        } else if (object instanceof Double) {
            return KServeDatatype.FP64;
        } else if (object instanceof Boolean) {
            return KServeDatatype.BOOL;
        } else if (object instanceof String | object instanceof Byte) {
            return KServeDatatype.BYTES;
        } else {
            throw new IllegalArgumentException("Cannot infer KServe type of " + object);
        }
    }

    public static KServeDatatype trustyToKserveType(Type type, Value value) throws IllegalArgumentException {
        final Object object = value.getUnderlyingObject();
        if (type == NUMBER) {
            if (object instanceof Integer) {
                return KServeDatatype.INT32;
            } else if (object instanceof Double) {
                return KServeDatatype.FP64;
            } else if (object instanceof Long) {
                return KServeDatatype.INT64;
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
            }
        } else if (type == BOOLEAN) {
            return KServeDatatype.BOOL;
        } else if (type == CATEGORICAL) {
            return KServeDatatype.BYTES;
        } else {
            throw new IllegalArgumentException("Unsupported TrustyAI type: " + type);
        }
    }

    static List<String> labelTensors(String name, int idxs) {
        return IntStream.range(0, idxs).mapToObj(i -> name + "-" + i).collect(Collectors.toList());
    }

    // === TENSOR VALUE UTILITIES ======================================================================================
    static List<?> getNthSlice(List l, int sliceIdx, int shapeProduct){
        return IntStream.range(0, l.size()).filter(i -> i%shapeProduct==sliceIdx).mapToObj(l::get).toList();
    }

    // === FEATURE HANDLING ============================================================================================
    static Feature contentsToFeature(ModelInferRequest.InferInputTensor tensor, String name, int index) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        int contentsCount = 0;
        try {
            switch (type) {
                case BOOL:
                    contentsCount = tensorContents.getBoolContentsCount();
                    return FeatureFactory.newBooleanFeature(name, tensorContents.getBoolContents(index));
                case INT8:
                case INT16:
                case INT32:
                    contentsCount = tensorContents.getIntContentsCount();
                    return FeatureFactory.newNumericalFeature(name, tensorContents.getIntContents(index));
                case INT64:
                    contentsCount = tensorContents.getInt64ContentsCount();
                    return FeatureFactory.newNumericalFeature(name, tensorContents.getInt64Contents(index));
                case FP32:
                    contentsCount = tensorContents.getFp32ContentsCount();
                    return FeatureFactory.newNumericalFeature(name, tensorContents.getFp32Contents(index));
                case FP64:
                    contentsCount = tensorContents.getFp64ContentsCount();
                    return FeatureFactory.newNumericalFeature(name, tensorContents.getFp64Contents(index));
                case BYTES:
                    contentsCount = tensorContents.getBytesContentsCount();
                    return FeatureFactory.newCategoricalFeature(name, String.valueOf(tensorContents.getBytesContents(index)));
                default:
                    throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Error in input-tensor parsing: Attempting to access index %d of input-tensor which only has length %d. This can happen if the tensor reports an incorrect shape.%nThe tensor that caused the error is shown below:%n%s",
                            index, contentsCount, tensor));
        }
    }

    // ==== TENSOR -> TRUSTYAI TENSOR FEATURE CONVERSIONS =====
    // create a feature containing the entirety of the inbound tensor
    static Feature contentsToTrustyAITensorFeature(ModelInferRequest.InferInputTensor tensor, String name) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        int[] shapeArray = tensor.getShapeList().stream().mapToInt(Long::intValue).toArray();
        switch (type) {
            case BOOL:
                Boolean[] boolData = tensorContents.getBoolContentsList().toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return FeatureFactory.newTensorFeature(name, boolTensor);
            case INT8:
            case INT16:
            case INT32:
                Integer[] intData = tensorContents.getIntContentsList().toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return FeatureFactory.newTensorFeature(name, intTensor);
            case INT64:
                Long[] longData = tensorContents.getInt64ContentsList().toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return FeatureFactory.newTensorFeature(name, longTensor);
            case FP32:
                Float[] floatData = tensorContents.getFp32ContentsList().toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return FeatureFactory.newTensorFeature(name, floatTensor);
            case FP64:
                Double[] doubleData = tensorContents.getFp64ContentsList().toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return FeatureFactory.newTensorFeature(name, doubleTensor);
            case BYTES:
                Byte[] byteData = tensorContents.getBytesContentsList().toArray(new Byte[0]);
                Tensor<Byte> byteTensor = new Tensor<>(byteData, shapeArray);
                return FeatureFactory.newTensorFeature(name, byteTensor);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    // create a feature containing the idx'th slice of the inbound tensor
    static Feature contentsToTrustyAITensorFeature(ModelInferRequest.InferInputTensor tensor, String name, int idx) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }

        // full list of shapes [batch, shape0, shape1, shape2...]
        List<Long> rawShapeList = tensor.getShapeList();

        // array of non-batch shapes [shape0, shape1, shape2...]
        int[] shapeArray = new int[rawShapeList.size()-1];

        // product of all non-batch-shapes
        int nonBatchShapesProduct = 1;
        for (int i=1; i<rawShapeList.size(); i++){
            nonBatchShapesProduct *= rawShapeList.get(i);
            shapeArray[i-1] = rawShapeList.get(i).intValue();
        }

        switch (type) {
            case BOOL:
                Boolean[] boolData = getNthSlice(tensorContents.getBoolContentsList(), idx, nonBatchShapesProduct).toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return FeatureFactory.newTensorFeature(name, boolTensor);
            case INT8:
            case INT16:
            case INT32:
                Integer[] intData =getNthSlice(tensorContents.getIntContentsList(), idx, nonBatchShapesProduct).toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return FeatureFactory.newTensorFeature(name, intTensor);
            case INT64:
                Long[] longData =  getNthSlice(tensorContents.getInt64ContentsList(), idx, nonBatchShapesProduct).toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return FeatureFactory.newTensorFeature(name, longTensor);
            case FP32:
                Float[] floatData = getNthSlice(tensorContents.getFp32ContentsList(), idx, nonBatchShapesProduct).toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return FeatureFactory.newTensorFeature(name, floatTensor);
            case FP64:
                Double[] doubleData = getNthSlice(tensorContents.getFp64ContentsList(), idx, nonBatchShapesProduct).toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return FeatureFactory.newTensorFeature(name, doubleTensor);
            case BYTES:
                Byte[] byteData = getNthSlice(tensorContents.getBytesContentsList(), idx, nonBatchShapesProduct).toArray(new Byte[0]);
                Tensor<Byte> byteTensor = new Tensor<>(byteData, shapeArray);
                return FeatureFactory.newTensorFeature(name, byteTensor);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }


    // === OUTPUT HANDLING =============================================================================================
    /**
     * Convert a {@link ModelInferResponse.InferOutputTensor} to a {@link Output}.
     * The datatype is inferred from the contents of the tensor, rather than the datatype field, since these can
     * sometimes be mismatched by the model itself.
     *
     * @param tensor A {@link ModelInferResponse.InferOutputTensor} to convert
     * @param name The name of the output
     * @param index The index of the output
     * @return An {@link Output} object
     */
    static Output contentsToOutput(ModelInferResponse.InferOutputTensor tensor, String name, int index) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();

        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }

        int contentsCount = 0;
        try {
            if (tensorContents.getBoolContentsCount() != 0) {
                return new Output(name, Type.BOOLEAN, new Value(tensorContents.getBoolContents(index)), Output.DEFAULT_SCORE);
            } else if (tensorContents.getIntContentsCount() != 0) {
                return new Output(name, Type.NUMBER, new Value(tensorContents.getIntContents(index)), Output.DEFAULT_SCORE);
            } else if (tensorContents.getInt64ContentsCount() != 0) {
                return new Output(name, Type.NUMBER, new Value(tensorContents.getInt64Contents(index)), Output.DEFAULT_SCORE);
            } else if (tensorContents.getFp32ContentsCount() != 0) {
                return new Output(name, Type.NUMBER, new Value(tensorContents.getFp32Contents(index)), Output.DEFAULT_SCORE);
            } else if (tensorContents.getFp64ContentsCount() != 0) {
                return new Output(name, Type.NUMBER, new Value(tensorContents.getFp64Contents(index)), Output.DEFAULT_SCORE);
            } else if (tensorContents.getBytesContentsCount() != 0) {
                return new Output(name, Type.CATEGORICAL, new Value(String.valueOf(tensorContents.getBytesContents(index))), Output.DEFAULT_SCORE);
            } else {
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Error in output-tensor parsing: Attempting to access index %d of output-tensor which only has length %d. This can happen if the tensor reports an incorrect shape.%nThe tensor that caused the error is shown below:%n%s",
                            index, contentsCount, tensor));
        }
    }

    // ==== TENSOR -> TRUSTYAI TENSOR OUTPUT CONVERSIONS =====
    static Output contentsToTrustyAITensorOutput(ModelInferResponse.InferOutputTensor tensor, String name) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
        int[] shapeArray = tensor.getShapeList().stream().mapToInt(Long::intValue).toArray();
        switch (type) {
            case BOOL:
                Boolean[] boolData = tensorContents.getBoolContentsList().toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(boolTensor), Output.DEFAULT_SCORE);
            case INT8:
            case INT16:
            case INT32:
                Integer[] intData = tensorContents.getIntContentsList().toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(intTensor), Output.DEFAULT_SCORE);
            case INT64:
                Long[] longData = tensorContents.getInt64ContentsList().toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(longTensor), Output.DEFAULT_SCORE);
            case FP32:
                Float[] floatData = tensorContents.getFp32ContentsList().toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(floatTensor), Output.DEFAULT_SCORE);
            case FP64:
                Double[] doubleData = tensorContents.getFp64ContentsList().toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(doubleTensor), Output.DEFAULT_SCORE);
            case BYTES:
                Byte[] byteData = tensorContents.getBytesContentsList().toArray(new Byte[0]);
                Tensor<Byte> byteTensor = new Tensor<>(byteData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(byteTensor), Output.DEFAULT_SCORE);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }

    // create an output containing the idx'th slice of the outbound response tensor
    static Output contentsToTrustyAITensorOutput(ModelInferResponse.InferOutputTensor tensor, String name, int idx) {
        final KServeDatatype type;
        InferTensorContents tensorContents = tensor.getContents();
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }

        // full list of shapes [batch, shape0, shape1, shape2...]
        List<Long> rawShapeList = tensor.getShapeList();

        // array of non-batch shapes [shape0, shape1, shape2...]
        int[] shapeArray = new int[rawShapeList.size()-1];

        // product of all non-batch-shapes
        int nonBatchShapesProduct = 1;
        for (int i=1; i<rawShapeList.size(); i++){
            nonBatchShapesProduct *= rawShapeList.get(i);
            shapeArray[i-1] = rawShapeList.get(i).intValue();
        }

        switch (type) {
            case BOOL:
                Boolean[] boolData = getNthSlice(tensorContents.getBoolContentsList(), idx, nonBatchShapesProduct).toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(boolTensor), Output.DEFAULT_SCORE);
            case INT8:
            case INT16:
            case INT32:
                Integer[] intData =getNthSlice(tensorContents.getIntContentsList(), idx, nonBatchShapesProduct).toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(intTensor), Output.DEFAULT_SCORE);
            case INT64:
                Long[] longData =  getNthSlice(tensorContents.getInt64ContentsList(), idx, nonBatchShapesProduct).toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(longTensor), Output.DEFAULT_SCORE);
            case FP32:
                Float[] floatData = getNthSlice(tensorContents.getFp32ContentsList(), idx, nonBatchShapesProduct).toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(floatTensor), Output.DEFAULT_SCORE);
            case FP64:
                Double[] doubleData = getNthSlice(tensorContents.getFp64ContentsList(), idx, nonBatchShapesProduct).toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(doubleTensor), Output.DEFAULT_SCORE);
            case BYTES:
                Byte[] byteData = getNthSlice(tensorContents.getBytesContentsList(), idx, nonBatchShapesProduct).toArray(new Byte[0]);
                Tensor<Byte> byteTensor = new Tensor<>(byteData, shapeArray);
                return new Output(name, Type.TENSOR, new Value(byteTensor), Output.DEFAULT_SCORE);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }
    // =================================================================================================================
    // ==== RAW HANDLERS ===============================================================================================
    // =================================================================================================================

    // === INPUTS =============================
    // converting an entire batch of raw contents is faster, so prefer this function when possible
    static List<PredictionInput> rawHandlerMulti(ModelInferRequest data, ModelInferRequest.InferInputTensor tensor, List<String> names, int secondShape, boolean raw) {
        if (raw) {
            return new ArrayList<>(List.of(RawPayloadParser.rawContentToPredictionInput(data, names)));
        }
        final List<Feature> feature = IntStream.range(0, secondShape)
                .mapToObj(i -> contentsToFeature(tensor, names.get(i), i))
                .collect(Collectors.toCollection(ArrayList::new));
        return List.of(new PredictionInput(feature));
    }

    // converting an entire batch of raw contents is faster, so prefer this function when possible
    static List<Feature> rawHandlerMultiFeature(ModelInferRequest data, ModelInferRequest.InferInputTensor tensor, List<String> names, int secondShape, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToPredictionInput(data, names).getFeatures();
        }
        return IntStream.range(0, secondShape)
                .mapToObj(i -> contentsToFeature(tensor, names.get(i), i))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // if only a single item from raw contents is needed, use this
    static Feature rawHandlerSingle(ModelInferRequest data, ModelInferRequest.InferInputTensor tensor, String name, int idx, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToFeature(data, name, 0, idx);
        } else {
            return contentsToFeature(tensor, name, idx);
        }
    }

    // convert a KServe tensor into a TrustyAI Tensor feature
    static Feature rawHandlerTrustyAITensor(ModelInferRequest data, ModelInferRequest.InferInputTensor tensor, String name, int idx, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToTrustyAITensorFeature(data, name, 0, idx);
        } else {
            return contentsToTrustyAITensorFeature(tensor, name, idx);
        }
    }

    // === OUTPUTS  =============================
    // if only a single item from raw contents is needed, use this
    static Output rawHandlerSingle(ModelInferResponse data, ModelInferResponse.InferOutputTensor tensor, String name, int tensorIdx, int idx, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToOutput(data, name, tensorIdx, idx);
        } else {
            return contentsToOutput(tensor, name, idx);
        }
    }

    // converting an entire batch of raw contents is faster, so prefer this function when possible
    static List<Output> rawHandlerMultiOutput(ModelInferResponse data, ModelInferResponse.InferOutputTensor tensor, List<String> names, int secondShape, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToPredictionOutput(data, names).getOutputs();
        }
        return IntStream.range(0, secondShape)
                .mapToObj(i -> contentsToOutput(tensor, names.get(i), i))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static List<PredictionOutput> rawHandlerMulti(ModelInferResponse data, ModelInferResponse.InferOutputTensor tensor, List<String> names, int secondShape, boolean raw) {
        return rawHandlerMulti(data, tensor, names, secondShape, raw, 0);
    }

    // converting an entire batch of raw contents is faster, so prefer this function when possible
    static List<PredictionOutput> rawHandlerMulti(ModelInferResponse data, ModelInferResponse.InferOutputTensor tensor, List<String> names, int secondShape, boolean raw, int idx) {
        if (raw) {
            return new ArrayList<>(List.of(RawPayloadParser.rawContentToPredictionOutput(data, names, idx)));
        }
        final List<Output> output = IntStream.range(0, secondShape)
                .mapToObj(i -> contentsToOutput(tensor, names.get(i), i))
                .collect(Collectors.toCollection(ArrayList::new));
        return List.of(new PredictionOutput(output));
    }

    // convert a KServe tensor into a TrustyAI Tensor output
    static Output rawHandlerTrustyAITensor(ModelInferResponse data, ModelInferResponse.InferOutputTensor tensor, String name, int idx, boolean raw) {
        if (raw) {
            return RawPayloadParser.rawContentToTrustyAITensorOutput(data, name, 0, idx);
        } else {
            return contentsToTrustyAITensorOutput(tensor, name, idx);
        }
    }

}
