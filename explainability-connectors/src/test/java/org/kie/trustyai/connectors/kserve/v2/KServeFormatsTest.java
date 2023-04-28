package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.trustyai.connectors.kserve.v2.TensorConverter.parseKserveModelInferRequest;
import static org.kie.trustyai.connectors.kserve.v2.TensorConverter.parseKserveModelInferResponse;

public class KServeFormatsTest {

    private static final String MODEL_ID = "example-1";
    private static final String MODEL_VERSION = "0.0.1";

    static Prediction generateSingleInputSingleOutputPrediction(String inputName, String outputName) {
        return new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature(inputName, 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output(outputName, Type.NUMBER, new Value(1.0), 1.0))));
    }

    static String generateRandomFeatureName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

    static List<String> generateRandomFeatureNames(String prefix, int number) {
        return IntStream.range(0, number).mapToObj(i -> generateRandomFeatureName(prefix)).collect(Collectors.toList());
    }

    static Prediction generateSingleInputMultiOutputPrediction(int nOutputFeatures, String inputName, String outputName) {
        return new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature(inputName, 10.0))),
                new PredictionOutput(IntStream.range(0, nOutputFeatures)
                        .mapToObj(i -> new Output(outputName + "-" + i, Type.NUMBER, new Value((double) i), 1.0))
                        .collect(Collectors.toUnmodifiableList())));
    }

    static Prediction generateMultiInputSingleOutputPrediction(int nInputFeatures, String inputName, String outputName) {
        return new SimplePrediction(
                new PredictionInput(
                        IntStream.range(0, nInputFeatures)
                                .mapToObj(i -> FeatureFactory.newNumericalFeature(inputName + "-" + i, (double) i * 10))
                                .collect(Collectors.toUnmodifiableList())),
                new PredictionOutput(
                        List.of(
                                new Output(outputName, Type.NUMBER, new Value(1.0), 1.0))));
    }

    static Prediction generateMultiInputMultiOutputPrediction(int nInputs, int nOutputs) {
        return new SimplePrediction(
                new PredictionInput(
                        IntStream.range(0, nInputs)
                                .mapToObj(i -> FeatureFactory.newNumericalFeature("f-" + i, (double) i * 10))
                                .collect(Collectors.toUnmodifiableList())),
                new PredictionOutput(
                        IntStream.range(0, nOutputs)
                                .mapToObj(i -> new Output("o-" + i, Type.NUMBER, new Value((double) i), 1.0))
                                .collect(Collectors.toUnmodifiableList())));
    }

    public static KServePayloads generateNPNoBatch(Prediction prediction, String inputTensorName, String outputTensorName) {
        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.rowAsSingleArrayInputTensor(0, inputTensorName);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.rowAsSingleArrayOutputTensor(0, outputTensorName);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        return new KServePayloads(request.build(), response.build());
    }

    public static KServePayloads generateNPBatch(Prediction prediction, int batchSize, String inputName, String outputName) {
        final List<Prediction> predictions = IntStream.range(0, batchSize).mapToObj(i -> prediction).collect(Collectors.toList());
        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.asArrayInputTensor(inputName);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.asArrayOutputTensor(outputName);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        return new KServePayloads(request.build(), response.build());
    }

    public static KServePayloads generatePDNoBatch(Prediction prediction) {
        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        List<ModelInferRequest.InferInputTensor.Builder> requestTensor = df.rowAsSingleDataframeInputTensor(0);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        requestTensor.forEach(request::addInputs);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final List<ModelInferResponse.InferOutputTensor.Builder> responseTensor = df.rowAsSingleDataframeOutputTensor(0);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        responseTensor.forEach(response::addOutputs);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        return new KServePayloads(request.build(), response.build());
    }

    public static KServePayloads generatePDBatch(Prediction prediction, int batchSize) {
        final List<Prediction> predictions = IntStream.range(0, batchSize).mapToObj(i -> prediction).collect(Collectors.toList());
        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        List<ModelInferRequest.InferInputTensor.Builder> requestTensor = df.asBatchDataframeInputTensor();
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        requestTensor.forEach(request::addInputs);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final List<ModelInferResponse.InferOutputTensor.Builder> responseTensor = df.asBatchDataframeOutputTensor();
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        responseTensor.forEach(response::addOutputs);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        return new KServePayloads(request.build(), response.build());
    }

    @RepeatedTest(5)
    void testSingleInputSingleOutputNPNoBatch() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");
        final KServePayloads payload = generateNPNoBatch(generateSingleInputSingleOutputPrediction(inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals(inputName + "-0", predictionInputs.get(0).getFeatures().get(0).getName());
        assertEquals(outputName + "-0", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputNPNoBatch(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");
        final KServePayloads payload = generateNPNoBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertEquals(inputName + "-0", predictionInputs.get(0).getFeatures().get(0).getName());
        for (int i = 0; i < nOutputs; i++) {
            assertEquals(outputName + "-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputNPNoBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals(inputName + "-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        assertEquals(outputName + "-0", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputNPNoBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals(inputName + "-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            assertEquals(outputName + "-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    // Start provided names tests

    @RepeatedTest(5)
    void testSingleInputSingleOutputNPNoBatchProvidedNames() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateSingleInputSingleOutputPrediction(
                inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(List.of("foo-1")));
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.of(List.of("bar-1")));
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals("foo-1", predictionInputs.get(0).getFeatures().get(0).getName());
        assertEquals("bar-1", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputNPNoBatchProvidedNames(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(List.of("foo-1")));
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(),
                Optional.of(IntStream.range(0, nOutputs).mapToObj(i -> "bar-" + i).collect(Collectors.toList())));
        assertEquals(1, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertEquals("foo-1", predictionInputs.get(0).getFeatures().get(0).getName());
        for (int i = 0; i < nOutputs; i++) {
            assertEquals("bar-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputNPNoBatchProvidedNames(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName), inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(),
                Optional.of(IntStream.range(0, nFeatures).mapToObj(i -> "foo-" + i).collect(Collectors.toList())));
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(),
                Optional.of(List.of("bar-1")));
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals("foo-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        assertEquals("bar-1", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputNPNoBatchProvidedNames(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPNoBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2),
                inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(),
                Optional.of(IntStream.range(0, nFeatures).mapToObj(i -> "foo-" + i).collect(Collectors.toList())));
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(),
                Optional.of(IntStream.range(0, nFeatures * 2).mapToObj(i -> "bar-" + i).collect(Collectors.toList())));
        assertEquals(1, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals("foo-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            assertEquals("bar-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    @RepeatedTest(5)
    void testSingleInputSingleOutputNPBatchProvidedNames() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateSingleInputSingleOutputPrediction(
                inputName, outputName), 10, inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(List.of("foo-1")));
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.of(List.of("bar-1")));
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals("foo-1", predictionInputs.get(0).getFeatures().get(0).getName());
        assertEquals("bar-1", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputNPBatchProvidedNames(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName), 10, inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(List.of("foo-1")));
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.of(
                IntStream.range(0, nOutputs).mapToObj(i -> "bar-" + i).collect(Collectors.toList())));
        assertEquals(10, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertTrue(predictionInputs.stream().allMatch(po -> po.getFeatures().get(0).getName().equals("foo-1")));
        for (int i = 0; i < nOutputs; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals("bar-" + n)));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputNPBatchProvidedNames(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName), 10, inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(
                IntStream.range(0, nFeatures).mapToObj(i -> "foo-" + i).collect(Collectors.toList())));
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.of(List.of("bar-1")));
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals("foo-" + n)));
        }
        assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(0).getName().equals("bar-1")));
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputNPBatchProvidedNames(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2), 10, inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.of(
                IntStream.range(0, nFeatures).mapToObj(i -> "foo-" + i).collect(Collectors.toList())));
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.of(
                IntStream.range(0, nFeatures * 2).mapToObj(i -> "bar-" + i).collect(Collectors.toList())));
        assertEquals(10, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals("foo-" + n)));
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals("bar-" + n)));
        }
    }

    // End provided names tests

    @RepeatedTest(5)
    void testSingleInputSingleOutputPDNoBatch() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDNoBatch(generateSingleInputSingleOutputPrediction(
                inputName, outputName));
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals(inputName, predictionInputs.get(0).getFeatures().get(0).getName());
        assertEquals(outputName, predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputPDNoBatch(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDNoBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName));
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertEquals(inputName, predictionInputs.get(0).getFeatures().get(0).getName());
        for (int i = 0; i < nOutputs; i++) {
            assertEquals(outputName + "-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputPDNoBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDNoBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName));
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals(inputName + "-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        assertEquals(outputName, predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputPDNoBatch(int nFeatures) {
        final KServePayloads payload = generatePDNoBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2));
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(1, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(1, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            assertEquals("f-" + i, predictionInputs.get(0).getFeatures().get(i).getName());
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            assertEquals("o-" + i, predictionOutputs.get(0).getOutputs().get(i).getName());
        }
    }

    @RepeatedTest(5)
    void testSingleInputSingleOutputNPBatch() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateSingleInputSingleOutputPrediction(
                inputName, outputName), 10, inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals(inputName + "-0", predictionInputs.get(0).getFeatures().get(0).getName());
        assertEquals(outputName + "-0", predictionOutputs.get(0).getOutputs().get(0).getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputNPBatch(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName), 10,
                inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(0).getName().equals(inputName + "-0")));
        for (int i = 0; i < nOutputs; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals(outputName + "-" + n)));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputNPBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName), 10,
                inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals(inputName + "-" + n)));
        }
        assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(0).getName().equals(outputName + "-0")));
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputNPBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generateNPBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2), 10,
                inputName, outputName);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals(inputName + "-" + n)));
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals(outputName + "-" + n)));
        }
    }

    @RepeatedTest(5)
    void testSingleInputSingleOutputPDBatch() {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDBatch(generateSingleInputSingleOutputPrediction(
                inputName, outputName), 10);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.empty(), true);
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.empty(), true);
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(0).getName().equals(inputName)));
        assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(0).getName().equals(outputName)));
    }

    //

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testSingleInputMultiOutputPDBatch(int nOutputs) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDBatch(generateSingleInputMultiOutputPrediction(nOutputs, inputName, outputName), 10);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput(), Optional.empty(), true);
        assertEquals(10, predictionInputs.size());
        assertEquals(1, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(nOutputs, predictionOutputs.get(0).getOutputs().size());
        assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(0).getName().equals(inputName)));
        for (int i = 0; i < nOutputs; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals(outputName + "-" + n)));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputSingleOutputPDBatch(int nFeatures) {
        final String inputName = generateRandomFeatureName("input");
        final String outputName = generateRandomFeatureName("output");

        final KServePayloads payload = generatePDBatch(generateMultiInputSingleOutputPrediction(nFeatures, inputName, outputName), 10);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput(), Optional.empty(), true);
        assertEquals(10, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals(inputName + "-" + n)));
        }
        assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(0).getName().equals(outputName)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 4, 5, 6, 7, 8, 9 })
    void testMultiInputMultiOutputPDBatch(int nFeatures) {
        final KServePayloads payload = generatePDBatch(generateMultiInputMultiOutputPrediction(nFeatures, nFeatures * 2), 10);
        final List<PredictionInput> predictionInputs = parseKserveModelInferRequest(payload.getInput());
        assertEquals(10, predictionInputs.size());
        assertEquals(nFeatures, predictionInputs.get(0).getFeatures().size());
        final List<PredictionOutput> predictionOutputs = parseKserveModelInferResponse(payload.getOutput());
        assertEquals(10, predictionOutputs.size());
        assertEquals(nFeatures * 2, predictionOutputs.get(0).getOutputs().size());
        for (int i = 0; i < nFeatures; i++) {
            final int n = i;
            assertTrue(predictionInputs.stream().allMatch(pi -> pi.getFeatures().get(n).getName().equals("f-" + n)));
        }
        for (int i = 0; i < nFeatures * 2; i++) {
            final int n = i;
            assertTrue(predictionOutputs.stream().allMatch(po -> po.getOutputs().get(n).getName().equals("o-" + n)));
        }
    }
}
