package org.kie.trustyai.service.endpoints.consumer.base;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.TensorDataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.reconcilers.ModelMeshInferencePayloadReconciler;
import org.kie.trustyai.service.endpoints.consumer.ConsumerEndpoint;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.partial.PartialKind;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.trustyai.service.PayloadProducer.MODEL_A_ID;

@QuarkusTest
@TestHTTPEndpoint(ConsumerEndpoint.class)
abstract class ConsumerEndpointBaseTest {
    private static final String MODEL_ID = "example-1";
    private static final String MODEL_VERSION = "0.0.1-TEST";
    private static final String INPUT_PREFIX = "inputs-lala";
    private static final String OUTPUT_PREFIX = "outputs-lala";

    private static final int BATCH_SIZE = 100;

    @Inject
    Instance<DataSource> datasource;

    @Inject
    ModelMeshInferencePayloadReconciler reconciler;

    abstract void resetDatasource() throws JsonProcessingException;

    abstract void clearStorage() throws JsonProcessingException;

    abstract String missingDataMessage(String modelId);

    /**
     * Empty the storage before each test.
     */
    @BeforeEach
    void emptyStorage() throws JsonProcessingException {
        clearStorage();
        resetDatasource();
        reconciler.clear();

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
        assertEquals(missingDataMessage(MODEL_A_ID), exception.getMessage());

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
        assertEquals(missingDataMessage(MODEL_A_ID), exception.getMessage());

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
    @Disabled("This failure case is only relevant for MariaDB testing")
    void consumePartialPostHuge() {

        // this should fail
        final InferencePartialPayload payload2 = PayloadProducer.getInferencePartialPayloadInput(UUID.randomUUID().toString(), 0, 25_000);
        given()
                .contentType(ContentType.JSON)
                .body(payload2)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("This can happen if the payload is too large, try reducing inference batch size."));
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
                .statusCode(RestResponse.StatusCode.OK);

        final InferencePartialPayload partialResponsePayloadBWrongSchema = new InferencePartialPayload();
        partialResponsePayloadBWrongSchema.setId(newId);
        partialResponsePayloadBWrongSchema.setData(PayloadProducer.getInferencePayloadB(0).getOutput());
        partialResponsePayloadBWrongSchema.setModelId(MODEL_A_ID);
        partialResponsePayloadBWrongSchema.setKind(PartialKind.response);
        given()
                .contentType(ContentType.JSON)
                .body(partialResponsePayloadBWrongSchema)
                .when().post().peek()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("Error when reconciling payload for response id='" + newId + "': Payload schema does not match stored schema for model=" + MODEL_A_ID + ": See mismatch description below:\n" +
                        "Output Schema mismatch:\n" +
                        "\tSchema column names do not match. Existing schema columns=[input-0], comparison schema columns=[output-0, output-1]"));
    }

    @Test
    void testConsumeImageInput() {
        int[] shape = { BATCH_SIZE, 3, 28, 28 };

        // send an image tensor as input
        final Random random = new Random(0);
        InferTensorContents.Builder contents = InferTensorContents.newBuilder();
        Double[][] arrayContents = new Double[shape[0]][shape[1] * shape[2] * shape[3]];
        int idx = 0;
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1] * shape[2] * shape[3]; j++) {
                double val = i + j / 10_000.;
                arrayContents[i][j] = val;
                contents.addFp64Contents(val);
                idx++;
            }
        }
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .setName("input")
                .addShape(shape[0])
                .addShape(shape[1])
                .addShape(shape[2])
                .addShape(shape[3])
                .setContents(contents).build();

        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
        request.addInputs(tensor);
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

        final List<Prediction> predictions = IntStream.range(0, BATCH_SIZE).mapToObj(i -> new SimplePrediction(
                new PredictionInput(List.of(FeatureFactory.newNumericalFeature("dummy", 10.0))),
                new PredictionOutput(
                        List.of(
                                new Output("output-1", Type.NUMBER, new Value(1.0), 1.0),
                                new Output("output-2", Type.NUMBER, new Value(2.0), 1.0)))))
                .collect(Collectors.toList());
        final TensorDataframe df = TensorDataframe.createFrom(predictions);

        // send two-output response
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
        assertEquals(3, storedDf.getColumnDimension());
        assertEquals(BATCH_SIZE, storedDf.getRowDimension());

        // check that the tensor is correctly retrieved

        for (int i = 0; i < BATCH_SIZE; i++) {
            Tensor<Double> nthTensor = (Tensor<Double>) storedDf.getValue(i, 0).getUnderlyingObject();
            assertArrayEquals(new int[] { 3, 28, 28 }, nthTensor.getDimensions());
            assertArrayEquals(arrayContents[i], nthTensor.getData());
        }
    }

    @Test
    void testConsumeImageInputOutput() {
        int[] shape = { BATCH_SIZE, 3, 28, 28 };

        //build input image ==========================
        final Random random = new Random(0);
        InferTensorContents.Builder inputContents = InferTensorContents.newBuilder();
        Double[][] inputContentsArray = new Double[shape[0]][shape[1] * shape[2] * shape[3]];
        int idx = 0;
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1] * shape[2] * shape[3]; j++) {
                double val = i + j / 10_000.;
                inputContentsArray[i][j] = val;
                inputContents.addFp64Contents(val);
                idx++;
            }
        }
        ModelInferRequest.InferInputTensor requestTensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .setName("input")
                .addShape(shape[0])
                .addShape(shape[1])
                .addShape(shape[2])
                .addShape(shape[3])
                .setContents(inputContents).build();

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

        //build output image ==========================
        InferTensorContents.Builder outputContents = InferTensorContents.newBuilder();
        Double[][] outputContentsArray = new Double[shape[0]][shape[1] * shape[2] * shape[3]];
        idx = 0;
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1] * shape[2] * shape[3]; j++) {
                double val = -i - j / 10_000.;
                outputContentsArray[i][j] = val;
                inputContents = outputContents.addFp64Contents(val);
                idx++;
            }
        }
        ModelInferResponse.InferOutputTensor responseTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .setName("input")
                .addShape(shape[0])
                .addShape(shape[1])
                .addShape(shape[2])
                .addShape(shape[3])
                .setContents(outputContents).build();
        final ModelInferResponse.Builder response = ModelInferResponse.newBuilder().addOutputs(responseTensor);
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
        assertEquals(2, storedDf.getColumnDimension());
        assertEquals(BATCH_SIZE, storedDf.getRowDimension());

        // check that the tensor is correctly retrieved

        for (int i = 0; i < BATCH_SIZE; i++) {
            Tensor<Double> nthInputTensor = (Tensor<Double>) storedDf.getValue(i, 0).getUnderlyingObject();
            assertArrayEquals(new int[] { 3, 28, 28 }, nthInputTensor.getDimensions());
            assertArrayEquals(inputContentsArray[i], nthInputTensor.getData());

            Tensor<Double> nthOutputTensor = (Tensor<Double>) storedDf.getValue(i, 1).getUnderlyingObject();
            assertArrayEquals(new int[] { 3, 28, 28 }, nthOutputTensor.getDimensions());
            assertArrayEquals(outputContentsArray[i], nthOutputTensor.getData());
        }
    }
}
