package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TensorConverter {

    private static final Logger logger = LoggerFactory.getLogger(TensorConverter.class);

    // =================================================================================================================
    // === REQUESTS ====================================================================================================
    public static List<PredictionInput> parseKserveModelInferRequest(ModelInferRequest data) {
        return parseKserveModelInferRequest(data, Optional.empty(), false);
    }

    public static List<PredictionInput> parseKserveModelInferRequest(ModelInferRequest data, Optional<List<String>> featureNames) {
        return parseKserveModelInferRequest(data, featureNames, false);
    }

    public static List<PredictionInput> parseKserveModelInferRequest(ModelInferRequest data, Optional<List<String>> featureNames, boolean isBatch) {
        final int count = Math.max(data.getInputsCount(), data.getRawInputContentsCount());
        boolean raw = data.getRawInputContentsCount() > 0;

        if (count == 1) { // The NP codec case
            final ModelInferRequest.InferInputTensor tensor = data.getInputs(0);
            List<Long> shape = tensor.getShapeList();

            final int firstShape = shape.get(0).intValue();

            if (firstShape < 2) {
                if (shape.size() == 2) {
                    // NP features, no batch
                    int secondShape = shape.get(1).intValue();
                    List<String> names;
                    if (isBatch) {
                        List<Feature> fs = TensorConverterUtils.rawHandlerMultiFeature(
                                data,
                                tensor,
                                Collections.nCopies(secondShape, tensor.getName()),
                                secondShape,
                                raw);
                        return fs.stream().map(f -> new PredictionInput(List.of(f))).collect(Collectors.toList());
                    } else {
                        if (featureNames.isPresent()) {
                            names = featureNames.get();
                        } else {
                            names = TensorConverterUtils.labelTensors(tensor.getName(), secondShape);
                        }
                        return TensorConverterUtils.rawHandlerMulti(data, tensor, names, secondShape, raw);
                    }
                } else if (shape.size() > 2 && !raw) { // raw handling not yet implemented
                    // tensor data -> entire tensor is one feature
                    List<Feature> fs = new ArrayList<>();
                    fs.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, tensor, tensor.getName(), 0, raw));
                    return List.of(new PredictionInput(fs));
                } else if (shape.size() == 1) {
                    // A single element feature, no batch. PD or NP irrelevant
                    List<Feature> fs = new ArrayList<>();
                    fs.add(TensorConverterUtils.rawHandlerSingle(data, tensor, tensor.getName(), 0, raw));
                    return List.of(new PredictionInput(fs));
                } else {
                    throw new IllegalArgumentException("Shape size not supported for tabular data");
                }
            } else {
                // NP-batch
                final List<PredictionInput> predictionInputs = new ArrayList<>();
                if (shape.size() == 1) {
                    for (int batch = 0; batch < firstShape; batch++) {
                        final List<Feature> features = new ArrayList<>();
                        String name = featureNames.isPresent() ? featureNames.get().get(0) : tensor.getName();
                        features.add(TensorConverterUtils.rawHandlerSingle(data, tensor, name, batch, raw));
                        predictionInputs.add(new PredictionInput(features));
                    }
                } else if (shape.size() > 2 && !raw) { // raw handling not yet implemented {
                    // tensor data of size [batch, a, b, c...] -> list of $batch tensors of shape [a,b,c...]
                    for (int batch = 0; batch < firstShape; batch++) {
                        List<Feature> fs = new ArrayList<>();
                        fs.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, tensor, tensor.getName(), batch, raw));
                        predictionInputs.add(new PredictionInput(fs));
                    }
                } else {
                    int secondShape = 1;
                    for (Long subShape : shape.subList(1, shape.size())) {
                        secondShape *= subShape.intValue();
                    }

                    for (int batch = 0; batch < firstShape; batch++) {
                        final List<Feature> features = new ArrayList<>();
                        for (int featureIndex = 0; featureIndex < secondShape; featureIndex++) {
                            final String name = featureNames.isPresent() ? featureNames.get().get(featureIndex) : tensor.getName() + "-" + featureIndex;
                            final int idx = secondShape * batch + featureIndex;
                            features.add(TensorConverterUtils.rawHandlerSingle(data, tensor, name, idx, raw));
                        }
                        predictionInputs.add(new PredictionInput(features));
                    }
                }
                logger.debug("Using NP codec (batch)");

                return predictionInputs;
            }

        } else if (count > 1) { // The PD codec case
            final List<Long> shape = data.getInputs(0).getShapeList();
            if (shape.size() < 2) {
                // Multi-feature PD, no batch
                logger.debug("Using PD codec (no batch)");
                final List<ModelInferRequest.InferInputTensor> tensors = data.getInputsList();
                final List<Feature> features = tensors.stream().map(tensor -> {
                    return TensorConverterUtils.rawHandlerSingle(data, tensor, tensor.getName(), 0, raw);
                }).collect(Collectors.toCollection(ArrayList::new));

                return new ArrayList<>(List.of(new PredictionInput(features)));
            } else if (shape.size() > 2 && !raw){
                List<PredictionInput> predictionInputs = new ArrayList<>();
                for (int batch = 0; batch < data.getInputsCount(); batch++) {
                    List<Feature> fs = new ArrayList<>();
                    fs.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, data.getInputs(batch), data.getInputs(batch).getName(), 0, raw));
                    predictionInputs.add(new PredictionInput(fs));
                }
                return predictionInputs;
            } else {
                // given some shape (ntensors, a, b, ... n)
                // return ntensors of PredictionOutputs, each with a*b*c*...*n outputs

                // Multi-feature PD, batch
                logger.debug("Using NP codec (batch)");
                final int secondShape = shape.get(1).intValue();
                final List<ModelInferRequest.InferInputTensor> tensors = data.getInputsList();
                final List<List<Feature>> features = tensors.stream().map(tensor -> {
                    return IntStream.range(0, secondShape)
                            .mapToObj(i -> TensorConverterUtils.rawHandlerSingle(data, tensor, tensor.getName(), i, raw))
                            .collect(Collectors.toCollection(ArrayList::new));
                }).collect(Collectors.toCollection(ArrayList::new));
                // Transpose the features
                final List<PredictionInput> predictionInputs = new ArrayList<>();
                for (int batch = 0; batch < secondShape; batch++) {
                    final List<Feature> batchFeatures = new ArrayList<>();
                    for (int featureIndex = 0; featureIndex < tensors.size(); featureIndex++) {
                        batchFeatures.add(features.get(featureIndex).get(batch));
                    }
                    predictionInputs.add(new PredictionInput(batchFeatures));
                }
                return predictionInputs;
            }
        } else {
            throw new IllegalArgumentException("Data inputs count not supported: " + count);
        }
    }

    // =================================================================================================================
    // === RESPONSES ===================================================================================================
    public static List<PredictionOutput> parseKserveModelInferResponse(ModelInferResponse data, int enforcedFirstDimension) {
        return parseKserveModelInferResponse(data, enforcedFirstDimension, Optional.empty(), false);
    }

    public static List<PredictionOutput> parseKserveModelInferResponse(ModelInferResponse data, int enforcedFirstDimension, Optional<List<String>> featureNames) {
        return parseKserveModelInferResponse(data, enforcedFirstDimension, featureNames, false);
    }

    public static List<PredictionOutput> parseKserveModelInferResponse(ModelInferResponse data, int enforcedFirstDimension, Optional<List<String>> featureNames, boolean isBatch) {
        final int count = Math.max(data.getOutputsCount(), data.getRawOutputContentsCount());
        boolean raw = data.getRawOutputContentsCount() > 0;

        if (count == 1) { // The NP codec case
            final ModelInferResponse.InferOutputTensor tensor = data.getOutputs(0);
            final List<Long> shape = tensor.getShapeList();
            final int firstShape = shape.get(0).intValue();

            if (firstShape < 2) {
                if (shape.size() == 2) {
                    // NP outputs, no batch
                    int secondShape = shape.get(1).intValue();
                    List<String> names;
                    if (isBatch) {
                        logger.debug("Using NP codec (batch)");
                        names = Collections.nCopies(secondShape, tensor.getName());
                        List<Output> os = TensorConverterUtils.rawHandlerMultiOutput(data, tensor, names, secondShape, raw);
                        return os.stream().map(o -> new PredictionOutput(List.of(o))).collect(Collectors.toList());
                    } else {
                        if (featureNames.isPresent()) {
                            names = featureNames.get();
                        } else {
                            names = IntStream.range(0, secondShape).mapToObj(i -> tensor.getName() + "-" + i).collect(Collectors.toCollection(ArrayList::new));
                        }
                        return TensorConverterUtils.rawHandlerMulti(data, tensor, names, secondShape, raw);
                    }
                } else if (shape.size() > 2) {
                    List<Output> os = new ArrayList<>();
                    os.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, tensor, tensor.getName(), 0, raw));
                    return List.of(new PredictionOutput(os));
                } else if (shape.size() == 1) {
                    // A single element feature, no batch. PD or NP irrelevant
                    return List.of(new PredictionOutput(List.of(TensorConverterUtils.rawHandlerSingle(data, tensor, tensor.getName(), 0, 0, raw))));
                } else {
                    throw new IllegalArgumentException("Shape size not supported for tabular data");
                }
            } else {
                // NP-batch
                final List<PredictionOutput> predictionOutputs = new ArrayList<>();
                if (shape.size() == 1) {
                    for (int batch = 0; batch < firstShape; batch++) {
                        final List<Output> outputs = new ArrayList<>();
                        String name = featureNames.isPresent() ? featureNames.get().get(0) : tensor.getName();
                        outputs.add(TensorConverterUtils.rawHandlerSingle(data, tensor, name, 0, batch, raw));
                        predictionOutputs.add(new PredictionOutput(outputs));
                    }
                } else if (shape.size() > 2 && !raw) { // raw handling not yet implemented {
                    // tensor data of size [batch, a, b, c...] -> list of $batch tensors of shape [a,b,c...]
                    for (int batch = 0; batch < firstShape; batch++) {
                        List<Output> os = new ArrayList<>();
                        os.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, tensor, tensor.getName(), batch, raw));
                        predictionOutputs.add(new PredictionOutput(os));
                    }
                } else {
                    logger.debug("Using NP codec (batch)");
                    int secondShape = 1;
                    for (Long subShape : shape.subList(1, shape.size())) {
                        secondShape *= subShape.intValue();
                    }

                    for (int batch = 0; batch < firstShape; batch++) {
                        final List<Output> outputs = new ArrayList<>();
                        for (int featureIndex = 0; featureIndex < secondShape; featureIndex++) {
                            String name = featureNames.isPresent() ? featureNames.get().get(featureIndex) : tensor.getName() + "-" + featureIndex;
                            outputs.add(TensorConverterUtils.rawHandlerSingle(data, tensor, name, 0, secondShape * batch + featureIndex, raw));
                        }
                        predictionOutputs.add(new PredictionOutput(outputs));
                    }
                }
                return predictionOutputs;
            }

        } else if (count > 1) { // The PD codec case
            // Multi-feature PD, batch
            logger.debug("Using NP codec (batch)");

            final List<ModelInferResponse.InferOutputTensor> tensors = data.getOutputsList();
            List<Integer> perTensorSecondShape = new ArrayList<>();
            List<List<Long>> perTensorShapes = new ArrayList<>();

            for (int tensorIDX = 0; tensorIDX < tensors.size(); tensorIDX++) {
                List<Long> perTensorShape = tensors.get(tensorIDX).getShapeList();
                perTensorSecondShape.add(1);
                perTensorShapes.add(perTensorShape);
                for (int i = 1; i < perTensorShape.size(); i++) {
                    perTensorSecondShape.set(tensorIDX, perTensorSecondShape.get(tensorIDX) * perTensorShape.get(i).intValue());
                }
            }

            // non-TrustyAI tensor case
            if (perTensorShapes.stream().allMatch(shapeList -> shapeList.size() <= 2)) {
                // given some shape (ntensors, a, b, ... n)
                // return ntensors of PredictionOutputs, each with a*b*c*...*n outputs
                boolean secondDimMatch = perTensorSecondShape.stream().allMatch(i -> i == enforcedFirstDimension);
                boolean firstDimMatch = perTensorShapes.stream().allMatch(i -> i.get(0) == enforcedFirstDimension);


                if (enforcedFirstDimension == 1) {
                    final List<Output> outputs = IntStream.range(0, tensors.size())
                            .mapToObj(tensorIDX -> {
                                List<String> names = TensorConverterUtils.labelTensors(tensors.get(tensorIDX).getName(), perTensorSecondShape.get(tensorIDX));
                                return TensorConverterUtils.rawHandlerMulti(
                                        data,
                                        tensors.get(tensorIDX),
                                        names,
                                        perTensorSecondShape.get(tensorIDX),
                                        raw, tensorIDX).get(0).getOutputs();
                            }).flatMap(Collection::stream).collect(Collectors.toList());
                    return List.of(new PredictionOutput(outputs));

                } else if (tensors.size() > 1 && firstDimMatch) {
                    // list of tensors of shape [efd, n], [efd, m], ...
                    List<PredictionOutput> outputs = new ArrayList<>();
                    int nOutputs = 0;
                    for (int outputIdx = 0; outputIdx < enforcedFirstDimension; outputIdx++) {
                        List<Output> os = new ArrayList<>();
                        for (int tIdx = 0; tIdx < tensors.size(); tIdx++) {
                            List<String> names = TensorConverterUtils.labelTensors(tensors.get(tIdx).getName(), perTensorSecondShape.get(tIdx));
                            for (int i = 0; i < perTensorSecondShape.get(tIdx); i++) {
                                os.add(TensorConverterUtils.rawHandlerSingle(data, tensors.get(tIdx), names.get(i), tIdx, outputIdx * perTensorSecondShape.get(tIdx) + i, raw));
                            }
                        }
                        if (outputIdx == 0) {
                            nOutputs = os.size();
                        }
                        outputs.add(new PredictionOutput(os));
                    }

                    if (secondDimMatch) {
                        logger.warn(String.format(
                                "Output tensor(s) have ambiguous shape: %s. " +
                                        "The input payload contained %d datapoints. However, both the first dimension and the " +
                                        "product of subsequent dimensions for all inbound output tensors are also of size %d. " +
                                        "TrustyAI will assume that the first dimension is the batch dimension, and will parse this payload " +
                                        "as %d outputs of size %d.",
                                perTensorShapes, enforcedFirstDimension, enforcedFirstDimension, enforcedFirstDimension, nOutputs));
                    }
                    return outputs;
                } else if (secondDimMatch) {
                    List<List<Output>> outputs = tensors.stream()
                            .map(tensor -> IntStream.range(0, perTensorSecondShape.get(0))
                                    .mapToObj(i -> TensorConverterUtils.rawHandlerSingle(data, tensor, tensor.getName(), 0, i, raw))
                                    .collect(Collectors.toCollection(ArrayList::new)))
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Transpose the features
                    final List<PredictionOutput> predictionOutputs = new ArrayList<>();
                    for (int batch = 0; batch < perTensorSecondShape.get(0); batch++) {
                        final List<Output> batchOutputs = new ArrayList<>();
                        for (int outputIndex = 0; outputIndex < tensors.size(); outputIndex++) {
                            batchOutputs.add(outputs.get(outputIndex).get(batch));
                        }
                        predictionOutputs.add(new PredictionOutput(batchOutputs));
                    }
                    return predictionOutputs;
                } else {
                    throw new IllegalArgumentException("Tensor shapes=" + perTensorShapes + " were expected to match the input count=" + enforcedFirstDimension + " along either the first or second dimension, but does not.");
                }
            } else {
                //tensor case
                if (tensors.size() == enforcedFirstDimension) {
                    List<PredictionOutput> predictionOutputs = new ArrayList<>();
                    for (int batch = 0; batch < tensors.size(); batch++) {
                        List<Output> os = new ArrayList<>();
                        os.add(TensorConverterUtils.rawHandlerTrustyAITensor(data, tensors.get(batch), tensors.get(batch).getName(), 0, raw));
                        predictionOutputs.add(new PredictionOutput(os));
                    }
                    return predictionOutputs;
                } else {
                    throw new IllegalArgumentException("Tensor count=" + tensors.size() + " was expected to match the input count=" + enforcedFirstDimension + ", but does not.");
                }
            }

        } else {
            throw new IllegalArgumentException("Data outputs count not supported: " + count);
        }
    }

}
