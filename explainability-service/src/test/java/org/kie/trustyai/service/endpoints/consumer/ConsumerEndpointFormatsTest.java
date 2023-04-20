package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;
import org.kie.trustyai.service.utils.KServePayloads;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointFormatsTest {

    private static final String MODEL_ID = "example-1";
    private static final String MODEL_VERSION = "0.0.1";
    private static final String INPUT_PREFIX = "inputs";
    private static final String OUTPUT_PREFIX = "outputs";

    private static final int BATCH_SIZE = 100;
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    static Prediction generateSingleInputSingleOutputPrediction() {
        return new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output("o-1", Type.NUMBER, new Value(1.0), 1.0))));
    }

    static Prediction generateSingleInputMultiOutputPrediction(int nOutputFeatures) {
        return new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0))),
                new PredictionOutput(IntStream.range(0, nOutputFeatures)
                        .mapToObj(i -> new Output("o-" + i, Type.NUMBER, new Value((double) i), 1.0))
                        .collect(Collectors.toUnmodifiableList())));
    }

    static Prediction generateMultiInputSingleOutputPrediction(int nInputFeatures) {
        return new SimplePrediction(
                new PredictionInput(
                        IntStream.range(0, nInputFeatures)
                                .mapToObj(i -> FeatureFactory.newNumericalFeature("f-" + i, (double) i * 10))
                                .collect(Collectors.toUnmodifiableList())),
                new PredictionOutput(
                        List.of(
                                new Output("o-1", Type.NUMBER, new Value(1.0), 1.0))));
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

    public static KServePayloads generateNPNoBatch(Prediction prediction) {
        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.rowAsSingleArrayInputTensor(0, INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.rowAsSingleArrayOutputTensor(0, OUTPUT_PREFIX);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        return new KServePayloads(request.build(), response.build());
    }

    public static KServePayloads generateNPBatch(Prediction prediction, int batchSize) {
        final List<Prediction> predictions = IntStream.range(0, batchSize).mapToObj(i -> prediction).collect(Collectors.toList());
        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.asArrayInputTensor(INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.asArrayOutputTensor(OUTPUT_PREFIX);
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

    /**
     * Empty the storage before each test.
     */
    @BeforeEach
    void emptyStorage() {
        datasource.get().empty();
        storage.get().emptyStorage();
    }

    /**
     * Scenario 1:
     *
     * Consume a single payload with a single input and multiple outputs, using the NP codec.
     * The input corresponds to:
     *
     * <pre>
     * model_name: "example-1"
     * model_version: "0.0.1"
     * inputs {
     *   name: "input"
     *   datatype: "FP64"
     *   shape: 1
     *   shape: 1
     *   contents {
     *     fp64_contents: 10.0
     *   }
     * }
     * </pre>
     * 
     * and the output corresponds to:
     * 
     * <pre>
     * model_name: "example-1"
     * model_version: "0.0.1"
     * outputs {
     *   name: "outputs"
     *   datatype: "FP64"
     *   shape: 1
     *   shape: 2
     *   contents {
     *     fp64_contents: 1.0
     *     fp64_contents: 2.0
     *   }
     * }
     * </pre>
     */
    @Test
    void consumeSingleInputMultiOutputNPCodecNoBatch() {
        final Prediction prediction = new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output("output-1", Type.NUMBER, new Value(1.0), 1.0),
                                new Output("output-2", Type.NUMBER, new Value(2.0), 1.0))));

        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.rowAsSingleArrayInputTensor(0, INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.rowAsSingleArrayOutputTensor(0, OUTPUT_PREFIX);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);
        // Assert that only one row has been stored
        assertEquals(1, storedDf.getRowDimension());
        // Assert the inputs and outputs have correct shape
        assertEquals(prediction.getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(prediction.getOutput().getOutputs().size(), storedDf.getOutputsCount());

        // Assert values are correct
        assertEquals(prediction.getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(1).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());

        // Assert names are correct
        assertEquals(INPUT_PREFIX + "-0", storedDf.getInputNames().get(0));
        assertEquals(OUTPUT_PREFIX + "-1", storedDf.getOutputNames().get(1));
        assertEquals(OUTPUT_PREFIX + "-0", storedDf.getOutputNames().get(0));

    }

    @Test
    void consumeMultiInputSingleOutputNPCodecNoBatch() {
        final Prediction prediction = new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0),
                                FeatureFactory.newNumericalFeature("f-2", 20.0))),
                new PredictionOutput(
                        List.of(
                                new Output("output-1", Type.NUMBER, new Value(2.0), 1.0))));

        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.rowAsSingleArrayInputTensor(0, INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId("");

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.rowAsSingleArrayOutputTensor(0, OUTPUT_PREFIX);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);
        assertEquals(prediction.getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(prediction.getOutput().getOutputs().size(), storedDf.getOutputsCount());
        assertEquals(1, storedDf.getRowDimension());
        assertEquals(prediction.getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(prediction.getInput().getFeatures().get(1).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());
    }

    @Test
    void consumeSingleInputMultiOutputPDCodecNoBatch() {
        final Prediction prediction = new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output("output-1", Type.NUMBER, new Value(1.0), 1.0),
                                new Output("output-2", Type.NUMBER, new Value(2.0), 1.0))));

        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        List<ModelInferRequest.InferInputTensor.Builder> requestTensors = df.rowAsSingleDataframeInputTensor(0);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        requestTensors.forEach(request::addInputs);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final List<ModelInferResponse.InferOutputTensor.Builder> responseTensors = df.rowAsSingleDataframeOutputTensor(0);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        responseTensors.forEach(response::addOutputs);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);
        assertEquals(prediction.getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(prediction.getOutput().getOutputs().size(), storedDf.getOutputsCount());
        assertEquals(1, storedDf.getRowDimension());
        assertEquals(prediction.getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(1).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());
    }

    /**
     * Scenario: 2 inputs, 1 output, PD codec, no batch
     *
     * The input corresponds to:
     * 
     * <pre>
     * model_version: "0.0.1"
     * inputs {
     *   name: "f-1"
     *   datatype: "FP64"
     *   shape: 1
     *   contents {
     *     fp64_contents: 10.0
     *   }
     * }
     * inputs {
     *   name: "f-2"
     *   datatype: "FP64"
     *   shape: 1
     *   contents {
     *     fp64_contents: 20.0
     *   }
     * }
     * </pre>
     *
     */
    @Test
    void consumeMultiInputSingleOutputPDCodecNoBatch() {
        final Prediction prediction = new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0),
                                FeatureFactory.newNumericalFeature("f-2", 20.0))),
                new PredictionOutput(
                        List.of(
                                new Output("o-1", Type.NUMBER, new Value(2.0), 1.0))));

        final TensorDataframe df = TensorDataframe.createFrom(List.of(prediction));

        List<ModelInferRequest.InferInputTensor.Builder> requestTensors = df.rowAsSingleDataframeInputTensor(0);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        requestTensors.forEach(request::addInputs);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final List<ModelInferResponse.InferOutputTensor.Builder> responseTensors = df.rowAsSingleDataframeOutputTensor(0);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        responseTensors.forEach(response::addOutputs);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);

        // Assert that only one row has been stored
        assertEquals(1, storedDf.getRowDimension());
        // Assert the inputs and outputs have correct shape
        assertEquals(prediction.getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(prediction.getOutput().getOutputs().size(), storedDf.getOutputsCount());

        // Assert values are correct
        assertEquals(prediction.getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(prediction.getInput().getFeatures().get(1).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(prediction.getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());

        // Assert names are correct
        assertEquals(prediction.getInput().getFeatures().get(0).getName(), storedDf.getInputNames().get(0));
        assertEquals(prediction.getInput().getFeatures().get(1).getName(), storedDf.getInputNames().get(1));
        assertEquals(prediction.getOutput().getOutputs().get(0).getName(), storedDf.getOutputNames().get(0));
    }

    // Batch tests

    @Test
    void consumeSingleInputMultiOutputNPCodecBatch() {

        final List<Prediction> predictions = IntStream.range(0, BATCH_SIZE).mapToObj(i -> new SimplePrediction(
                new PredictionInput(
                        List.of(FeatureFactory.newNumericalFeature("f-1", 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output("output-1", Type.NUMBER, new Value(1.0), 1.0),
                                new Output("output-2", Type.NUMBER, new Value(2.0), 1.0)))))
                .collect(Collectors.toList());

        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        ModelInferRequest.InferInputTensor.Builder requestTensor = df.asArrayInputTensor(INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.asArrayOutputTensor(OUTPUT_PREFIX);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);
        // Assert that only one row has been stored
        assertEquals(BATCH_SIZE, storedDf.getRowDimension());
        // Assert the inputs and outputs have correct shape
        assertEquals(predictions.get(0).getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(predictions.get(0).getOutput().getOutputs().size(), storedDf.getOutputsCount());

        // Assert values are correct
        assertEquals(predictions.get(0).getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(predictions.get(0).getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(predictions.get(0).getOutput().getOutputs().get(1).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());

        // Assert names are correct
        assertEquals(INPUT_PREFIX + "-0", storedDf.getInputNames().get(0));
        assertEquals(OUTPUT_PREFIX + "-1", storedDf.getOutputNames().get(1));
        assertEquals(OUTPUT_PREFIX + "-0", storedDf.getOutputNames().get(0));

    }

    @Test
    void consumeMultiInputSingleOutputNPCodecBatch() {
        final List<Prediction> predictions = IntStream.range(0, BATCH_SIZE).mapToObj(i -> {
            return new SimplePrediction(
                    new PredictionInput(
                            List.of(FeatureFactory.newNumericalFeature("f-1", 10.0),
                                    FeatureFactory.newNumericalFeature("f-2", 20.0))),
                    new PredictionOutput(
                            List.of(
                                    new Output("output-1", Type.NUMBER, new Value(2.0), 1.0))));
        }).collect(Collectors.toList());

        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        final ModelInferRequest.InferInputTensor.Builder requestTensor = df.asArrayInputTensor(INPUT_PREFIX);
        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(requestTensor);
        request.setModelName(MODEL_ID);
        request.setModelVersion(MODEL_VERSION);

        final String id = UUID.randomUUID().toString();

        InferencePartialPayload requestPayload = new InferencePartialPayload();
        requestPayload.setData(Base64.getEncoder().encodeToString(request.build().toByteArray()));
        requestPayload.setId(id);
        requestPayload.setKind(PartialKind.request);
        requestPayload.setModelId("");

        given()
                .contentType(ContentType.JSON)
                .body(requestPayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final ModelInferResponse.InferOutputTensor.Builder responseTensor = df.asArrayOutputTensor(OUTPUT_PREFIX);
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addOutputs(responseTensor);
        response.setModelName(MODEL_ID);
        response.setModelVersion(MODEL_VERSION);

        InferencePartialPayload responsePayload = new InferencePartialPayload();
        responsePayload.setData(Base64.getEncoder().encodeToString(response.build().toByteArray()));
        responsePayload.setId(id);
        responsePayload.setKind(PartialKind.response);
        responsePayload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(responsePayload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final Dataframe storedDf = datasource.get().getDataframe(MODEL_ID);
        assertEquals(predictions.get(0).getInput().getFeatures().size(), storedDf.getInputsCount());
        assertEquals(predictions.get(0).getOutput().getOutputs().size(), storedDf.getOutputsCount());
        assertEquals(BATCH_SIZE, storedDf.getRowDimension());
        assertEquals(predictions.get(0).getInput().getFeatures().get(0).getValue().asNumber(), storedDf.getValue(0, 0).asNumber());
        assertEquals(predictions.get(0).getInput().getFeatures().get(1).getValue().asNumber(), storedDf.getValue(0, 1).asNumber());
        assertEquals(predictions.get(0).getOutput().getOutputs().get(0).getValue().asNumber(), storedDf.getValue(0, 2).asNumber());
    }

}