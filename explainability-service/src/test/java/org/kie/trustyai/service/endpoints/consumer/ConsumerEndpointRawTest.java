package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.RawConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferParameter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;
import org.kie.trustyai.service.profiles.MemoryTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

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

/**
 * Test the {@link ConsumerEndpoint} with raw content payloads.
 */
@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointRawTest {

    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    /**
     * Create an input partial payload with no synthetic flag
     *
     * @param id
     * @return
     */
    private static InferencePartialPayload createInputFP64(UUID id) {
        return createInputFP64(id, false);
    }

    /**
     * Create an input partial payload with the specified synthetic flag
     *
     * @param id
     * @param synthetic
     * @return
     */
    private static InferencePartialPayload createInputFP64(UUID id, boolean synthetic) {
        final Random random = new Random(0);
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromDouble(values));
        ModelInferRequest.InferInputTensor.Builder tensorBuilder = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3);

        if (synthetic) {
            tensorBuilder.putParameters(ExplainerEndpoint.BIAS_IGNORE_PARAM, InferParameter.newBuilder().setStringParam("true").build());
        }
        ModelInferRequest.InferInputTensor tensor = tensorBuilder.build();

        builder.addInputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.request);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    private static InferencePartialPayload createOutputF64(UUID id) {
        final Random random = new Random(0);
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble());
        ModelInferResponse.Builder builder = ModelInferResponse.newBuilder();
        builder.addRawOutputContents(RawConverter.fromDouble(values));
        ModelInferResponse.InferOutputTensor tensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(2)
                .build();
        builder.addOutputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.response);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    private static InferencePartialPayload createInputINT64(UUID id) {
        final Random random = new Random(0);
        final List<Long> values = List.of(random.nextLong(), random.nextLong(), random.nextLong());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromLong(values));
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("INT64")
                .addShape(1).addShape(3)
                .build();
        builder.addInputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.request);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    private static InferencePartialPayload createOutputINT64(UUID id) {
        final Random random = new Random(0);
        final List<Long> values = List.of(random.nextLong(), random.nextLong());
        ModelInferResponse.Builder builder = ModelInferResponse.newBuilder();
        builder.addRawOutputContents(RawConverter.fromLong(values));
        ModelInferResponse.InferOutputTensor tensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("INT64")
                .addShape(1).addShape(2)
                .build();
        builder.addOutputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.response);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    private static InferencePartialPayload createInputINT32(UUID id) {
        final Random random = new Random(0);
        final List<Integer> values = List.of(random.nextInt(), random.nextInt(), random.nextInt());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromInteger(values));
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("INT32")
                .addShape(1).addShape(3)
                .build();
        builder.addInputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.request);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    private static InferencePartialPayload createOutputINT32(UUID id) {
        final Random random = new Random(0);
        final List<Integer> values = List.of(random.nextInt(), random.nextInt());
        ModelInferResponse.Builder builder = ModelInferResponse.newBuilder();
        builder.addRawOutputContents(RawConverter.fromInteger(values));
        ModelInferResponse.InferOutputTensor tensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("INT32")
                .addShape(1).addShape(2)
                .build();
        builder.addOutputs(tensor);
        builder.setModelName(MODEL_A_ID);
        builder.setModelVersion("1");
        builder.setId(id.toString());
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setKind(PartialKind.response);
        payload.setData(Base64.getEncoder().encodeToString(builder.build().toByteArray()));
        payload.setId(id.toString());
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    /**
     * Empty the storage before each test.
     */
    @BeforeEach
    void emptyStorage() throws JsonProcessingException {
        datasource.get().reset();
        storage.get().emptyStorage();
    }

    @Test
    void consumePartialPostInputsOnly() {

        final UUID id = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {

            final InferencePartialPayload payload = createInputFP64(id);

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
        final UUID id = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInputFP64(id);
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
    void consumePartialPostSomeFP64() {
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInputFP64(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 3; i++) {
            final InferencePartialPayload payload = createOutputF64(ids.get(i));
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
    @DisplayName("Synthetic payloads should be correctly tagged in the dataframe")
    void consumePartialPostFP64Synthetic() {
        final int N = 100;
        final int syntheticN = N - new Random().nextInt(N);

        final List<UUID> ids = IntStream.range(0, N).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < N - syntheticN; i++) {
            final InferencePartialPayload payload = createInputFP64(ids.get(i), false);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = N - syntheticN; i < N; i++) {
            final InferencePartialPayload payload = createInputFP64(ids.get(i), true);
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }

        for (int i = 0; i < N; i++) {
            final InferencePartialPayload payload = createOutputF64(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }

        final Dataframe dataframe = datasource.get().getDataframe(MODEL_A_ID);
        assertEquals(N, dataframe.getRowDimension());
        assertEquals(syntheticN, dataframe.filterRowsBySynthetic(true).getRowDimension());
        assertEquals(N - syntheticN, dataframe.filterRowsBySynthetic(false).getRowDimension());
    }

    @Test
    void consumePartialPostSomeINT64() {
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInputINT64(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 3; i++) {
            final InferencePartialPayload payload = createOutputF64(ids.get(i));
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
    void consumePartialPostSomeINT32() {
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInputINT32(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 3; i++) {
            final InferencePartialPayload payload = createOutputF64(ids.get(i));
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
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInputFP64(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createOutputF64(ids.get(i));
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
}
