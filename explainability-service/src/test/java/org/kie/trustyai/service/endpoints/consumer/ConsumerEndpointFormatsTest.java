package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.profiles.MemoryTestProfile;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.trustyai.service.PayloadProducer.MODEL_A_ID;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointFormatsTest {

    private static final String MODEL_ID = "example-1";
    private static final String MODEL_VERSION = "0.0.1-TEST";
    private static final String INPUT_PREFIX = "inputs-lala";
    private static final String OUTPUT_PREFIX = "outputs-lala";

    private static final int BATCH_SIZE = 100;
    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

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

    @Test
    void consumePartialPostInputsOnly() {
        final String id = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadInput(id, i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        Exception exception = assertThrows(DataframeCreateException.class, () -> {
            final Dataframe dataframe = datasource.get().getDataframe(MODEL_A_ID);
        });
        assertEquals("Data file '" + MODEL_A_ID + "-data.csv' not found", exception.getMessage());

    }

    @Test
    void consumePartialPostOutputsOnly() {
        final String id = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadOutput(id, i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        Exception exception = assertThrows(DataframeCreateException.class, () -> {
            final Dataframe dataframe = datasource.get().getDataframe(MODEL_A_ID);
        });
        assertEquals("Data file '" + MODEL_A_ID + "-data.csv' not found", exception.getMessage());

    }

    @Test
    void consumePartialPostSome() {
        final List<String> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadInput(ids.get(i), i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 3; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadOutput(ids.get(i), i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }

        final Dataframe dataframe = datasource.get().getDataframe(MODEL_A_ID);
        assertEquals(3, dataframe.getRowDimension());

    }

    @Test
    void consumePartialPostAll() {
        final List<String> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadInput(ids.get(i), i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = PayloadProducer.getInferencePartialPayloadOutput(ids.get(i), i);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }

        final Dataframe dataframe = datasource.get().getDataframe(MODEL_A_ID);
        assertEquals(5, dataframe.getRowDimension());

    }

    @Test
    void consumeDifferentSchemas() {
        final InferencePayload payloadModelA = PayloadProducer.getInferencePayloadA(0);
        final InferencePayload payloadModelB = PayloadProducer.getInferencePayloadB(0);

        // Generate two partial payloads with consistent metadata (from the same model)
        final String id = "This schema is OK";
        final InferencePartialPayload partialRequestPayloadA = new InferencePartialPayload();
        partialRequestPayloadA.setId(id);
        partialRequestPayloadA.setData(payloadModelA.getInput());
        partialRequestPayloadA.setModelId(MODEL_A_ID);
        partialRequestPayloadA.setKind(PartialKind.request);
        given()
                .contentType(ContentType.JSON)
                .body(partialRequestPayloadA)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));
        final InferencePartialPayload partialResponsePayloadA = new InferencePartialPayload();
        partialResponsePayloadA.setId(id);
        partialResponsePayloadA.setData(payloadModelA.getOutput());
        partialResponsePayloadA.setModelId(MODEL_A_ID);
        partialResponsePayloadA.setKind(PartialKind.response);
        given()
                .contentType(ContentType.JSON)
                .body(partialResponsePayloadA)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        // Generate two partial payloads with inconsistent metadata (from different models)
        final String newId = "This schema is NOT OK";
        final InferencePartialPayload partialRequestPayloadAWrongSchema = new InferencePartialPayload();
        partialRequestPayloadAWrongSchema.setId(newId);
        partialRequestPayloadAWrongSchema.setData(payloadModelA.getInput());
        partialRequestPayloadAWrongSchema.setModelId(MODEL_A_ID);
        partialRequestPayloadAWrongSchema.setKind(PartialKind.request);
        given()
                .contentType(ContentType.JSON)
                .body(partialRequestPayloadAWrongSchema)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is(""));

        final InferencePartialPayload partialResponsePayloadBWrongSchema = new InferencePartialPayload();
        partialResponsePayloadBWrongSchema.setId(newId);
        partialResponsePayloadBWrongSchema.setData(PayloadProducer.getInferencePayloadB(0).getOutput());
        partialResponsePayloadBWrongSchema.setModelId(MODEL_A_ID);
        partialResponsePayloadBWrongSchema.setKind(PartialKind.response);
        given()
                .contentType(ContentType.JSON)
                .body(partialResponsePayloadBWrongSchema)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("Invalid schema for payload response id=" + newId + ", Payload schema and stored schema are not the same"));
    }

}
