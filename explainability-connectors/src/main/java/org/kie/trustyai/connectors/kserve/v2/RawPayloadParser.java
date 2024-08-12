package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;

import org.kie.trustyai.connectors.kserve.KServeDatatype;
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

import com.google.protobuf.ByteString;

public class RawPayloadParser {

    public static PredictionInput rawContentToPredictionInput(ModelInferRequest request, List<String> inputNames) {
        return rawContentToPredictionInput(request, inputNames, 0);
    }

    public static PredictionInput rawContentToPredictionInput(ModelInferRequest request, List<String> inputNames, int idx) throws IllegalArgumentException {
        final ModelInferRequest.InferInputTensor tensor = request.getInputs(idx);
        final ByteString raw = request.getRawInputContents(idx);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        switch (type) {
            case BOOL:
                return PayloadParser.inputFromContentList(RawValueExtractor.toBoolean(raw), Type.BOOLEAN, inputNames);
            case INT8:
            case INT16:
            case INT32:
                return PayloadParser.inputFromContentList(RawValueExtractor.toInteger(raw), Type.NUMBER, inputNames);
            case INT64:
                return PayloadParser.inputFromContentList(RawValueExtractor.toLong(raw), Type.NUMBER, inputNames);
            case FP32:
                return PayloadParser.inputFromContentList(RawValueExtractor.toFloat(raw), Type.NUMBER, inputNames);
            case FP64:
                return PayloadParser.inputFromContentList(RawValueExtractor.toDouble(raw), Type.NUMBER, inputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    // this is less efficient than converting everything to a PredictionInput, so avoid when possible
    public static Feature rawContentToFeature(ModelInferRequest request, String inputName, int tensorIDX, int featureIDX) throws IllegalArgumentException {
        final ModelInferRequest.InferInputTensor tensor = request.getInputs(tensorIDX);
        final ByteString raw = request.getRawInputContents(tensorIDX);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        switch (type) {
            case BOOL:
                return new Feature(inputName, Type.BOOLEAN, new Value(RawValueExtractor.toBoolean(raw).get(featureIDX)));
            case INT8:
            case INT16:
            case INT32:
                return new Feature(inputName, Type.NUMBER, new Value(RawValueExtractor.toInteger(raw).get(featureIDX)));
            case INT64:
                return new Feature(inputName, Type.NUMBER, new Value(RawValueExtractor.toLong(raw).get(featureIDX)));
            case FP32:
                return new Feature(inputName, Type.NUMBER, new Value(RawValueExtractor.toFloat(raw).get(featureIDX)));
            case FP64:
                return new Feature(inputName, Type.NUMBER, new Value(RawValueExtractor.toDouble(raw).get(featureIDX)));
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    public static Feature rawContentToTrustyAITensorFeature(ModelInferRequest request, String inputName, int tensorIDX, int featureIDX) throws IllegalArgumentException {
        final ModelInferRequest.InferInputTensor tensor = request.getInputs(tensorIDX);
        final ByteString raw = request.getRawInputContents(tensorIDX);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }

        // full list of shapes [batch, shape0, shape1, shape2...]
        List<Long> rawShapeList = tensor.getShapeList();

        // array of non-batch shapes [shape0, shape1, shape2...]
        int[] shapeArray = new int[rawShapeList.size() - 1];
        int nonBatchShapesProduct = 1;
        for (int i = 1; i < rawShapeList.size(); i++) {
            nonBatchShapesProduct *= rawShapeList.get(i);
            shapeArray[i - 1] = rawShapeList.get(i).intValue();
        }

        switch (type) {
            case BOOL:
                List<Boolean> boolList = RawValueExtractor.toBoolean(raw);
                Boolean[] boolData = TensorConverterUtils.getNthSlice(boolList, featureIDX, nonBatchShapesProduct).toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return FeatureFactory.newTensorFeature(inputName, boolTensor);
            case INT8:
            case INT16:
            case INT32:
                List<Integer> intList = RawValueExtractor.toInteger(raw);
                Integer[] intData = TensorConverterUtils.getNthSlice(intList, featureIDX, nonBatchShapesProduct).toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return FeatureFactory.newTensorFeature(inputName, intTensor);
            case INT64:
                List<Long> longList = RawValueExtractor.toLong(raw);
                Long[] longData = TensorConverterUtils.getNthSlice(longList, featureIDX, nonBatchShapesProduct).toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return FeatureFactory.newTensorFeature(inputName, longTensor);
            case FP32:
                List<Float> floatList = RawValueExtractor.toFloat(raw);
                Float[] floatData = TensorConverterUtils.getNthSlice(floatList, featureIDX, nonBatchShapesProduct).toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return FeatureFactory.newTensorFeature(inputName, floatTensor);
            case FP64:
                List<Double> doubleList = RawValueExtractor.toDouble(raw);
                Double[] doubleData = TensorConverterUtils.getNthSlice(doubleList, featureIDX, nonBatchShapesProduct).toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return FeatureFactory.newTensorFeature(inputName, doubleTensor);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    public static PredictionOutput rawContentToPredictionOutput(ModelInferResponse response, List<String> outputNames) {
        return rawContentToPredictionOutput(response, outputNames, 0);
    }

    public static PredictionOutput rawContentToPredictionOutput(ModelInferResponse response,
            List<String> outputNames, int idx) throws IllegalArgumentException {
        final ModelInferResponse.InferOutputTensor tensor = response.getOutputs(idx);
        final ByteString raw = response.getRawOutputContents(idx);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }

        switch (type) {
            case BOOL:
                return PayloadParser.outputFromContentList(RawValueExtractor.toBoolean(raw), Type.BOOLEAN, outputNames);
            case INT8:
            case INT16:
            case INT32:
                return PayloadParser.outputFromContentList(RawValueExtractor.toInteger(raw), Type.NUMBER, outputNames);
            case INT64:
                return PayloadParser.outputFromContentList(RawValueExtractor.toLong(raw), Type.NUMBER, outputNames);
            case FP32:
                return PayloadParser.outputFromContentList(RawValueExtractor.toFloat(raw), Type.NUMBER, outputNames);
            case FP64:
                return PayloadParser.outputFromContentList(RawValueExtractor.toDouble(raw), Type.NUMBER, outputNames);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }

    // this is less efficient than converting everything to a PredictionOutput, so avoid when possible
    public static Output rawContentToOutput(ModelInferResponse request, String outputName, int tensorIDX, int outputIDX) throws IllegalArgumentException {
        final ModelInferResponse.InferOutputTensor tensor = request.getOutputs(tensorIDX);
        final ByteString raw = request.getRawOutputContents(tensorIDX);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
        switch (type) {
            case BOOL:
                return new Output(outputName, Type.BOOLEAN, new Value(RawValueExtractor.toBoolean(raw).get(outputIDX)), Output.DEFAULT_SCORE);
            case INT8:
            case INT16:
            case INT32:
                return new Output(outputName, Type.NUMBER, new Value(RawValueExtractor.toInteger(raw).get(outputIDX)), Output.DEFAULT_SCORE);
            case INT64:
                return new Output(outputName, Type.NUMBER, new Value(RawValueExtractor.toLong(raw).get(outputIDX)), Output.DEFAULT_SCORE);
            case FP32:
                return new Output(outputName, Type.NUMBER, new Value(RawValueExtractor.toFloat(raw).get(outputIDX)), Output.DEFAULT_SCORE);
            case FP64:
                return new Output(outputName, Type.NUMBER, new Value(RawValueExtractor.toDouble(raw).get(outputIDX)), Output.DEFAULT_SCORE);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor input, type=" + tensor.getDatatype());
        }
    }

    public static Output rawContentToTrustyAITensorOutput(ModelInferResponse request, String outputName, int tensorIDX, int outputIDX) throws IllegalArgumentException {
        final ModelInferResponse.InferOutputTensor tensor = request.getOutputs(tensorIDX);
        final ByteString raw = request.getRawOutputContents(tensorIDX);
        final KServeDatatype type;
        try {
            type = KServeDatatype.valueOf(tensor.getDatatype());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }

        // full list of shapes [batch, shape0, shape1, shape2...]
        List<Long> rawShapeList = tensor.getShapeList();

        // array of non-batch shapes [shape0, shape1, shape2...]
        int[] shapeArray = new int[rawShapeList.size() - 1];
        int nonBatchShapesProduct = 1;
        for (int i = 1; i < rawShapeList.size(); i++) {
            nonBatchShapesProduct *= rawShapeList.get(i);
            shapeArray[i - 1] = rawShapeList.get(i).intValue();
        }

        switch (type) {
            case BOOL:
                List<Boolean> boolList = RawValueExtractor.toBoolean(raw);
                Boolean[] boolData = TensorConverterUtils.getNthSlice(boolList, outputIDX, nonBatchShapesProduct).toArray(new Boolean[0]);
                Tensor<Boolean> boolTensor = new Tensor<>(boolData, shapeArray);
                return new Output(outputName, Type.TENSOR, new Value(boolTensor), Output.DEFAULT_SCORE);
            case INT8:
            case INT16:
            case INT32:
                List<Integer> intList = RawValueExtractor.toInteger(raw);
                Integer[] intData = TensorConverterUtils.getNthSlice(intList, outputIDX, nonBatchShapesProduct).toArray(new Integer[0]);
                Tensor<Integer> intTensor = new Tensor<>(intData, shapeArray);
                return new Output(outputName, Type.TENSOR, new Value(intTensor), Output.DEFAULT_SCORE);
            case INT64:
                List<Long> longList = RawValueExtractor.toLong(raw);
                Long[] longData = TensorConverterUtils.getNthSlice(longList, outputIDX, nonBatchShapesProduct).toArray(new Long[0]);
                Tensor<Long> longTensor = new Tensor<>(longData, shapeArray);
                return new Output(outputName, Type.TENSOR, new Value(longTensor), Output.DEFAULT_SCORE);
            case FP32:
                List<Float> floatList = RawValueExtractor.toFloat(raw);
                Float[] floatData = TensorConverterUtils.getNthSlice(floatList, outputIDX, nonBatchShapesProduct).toArray(new Float[0]);
                Tensor<Float> floatTensor = new Tensor<>(floatData, shapeArray);
                return new Output(outputName, Type.TENSOR, new Value(floatTensor), Output.DEFAULT_SCORE);
            case FP64:
                List<Double> doubleList = RawValueExtractor.toDouble(raw);
                Double[] doubleData = TensorConverterUtils.getNthSlice(doubleList, outputIDX, nonBatchShapesProduct).toArray(new Double[0]);
                Tensor<Double> doubleTensor = new Tensor<>(doubleData, shapeArray);
                return new Output(outputName, Type.TENSOR, new Value(doubleTensor), Output.DEFAULT_SCORE);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output, type=" + tensor.getDatatype());
        }
    }
}
