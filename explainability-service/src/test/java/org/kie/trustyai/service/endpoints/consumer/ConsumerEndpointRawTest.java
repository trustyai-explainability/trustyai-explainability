package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.RawConverterUtils;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.trustyai.service.PayloadProducer.MODEL_A_ID;

/**
 * Test the {@link ConsumerEndpoint} with raw content payloads.
 */
@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsumerEndpointRawTest {

    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    private static InferencePartialPayload createInput(UUID id) {
        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverterUtils.fromDouble(values));
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
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

    private static InferencePartialPayload createOutput(UUID id) {
        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble());
        ModelInferResponse.Builder builder = ModelInferResponse.newBuilder();
        builder.addRawOutputContents(RawConverterUtils.fromDouble(values));
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

    /**
     * Empty the storage before each test.
     */
    @BeforeEach
    void emptyStorage() {
        datasource.get().empty();
        storage.get().emptyStorage();
    }

    @Test
    void consumePartialPostInputsOnly() {

        final UUID id = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {

            final InferencePartialPayload payload = createInput(id);

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
            final InferencePartialPayload payload = createInput(id);
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
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createInput(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 3; i++) {
            final InferencePartialPayload payload = createOutput(ids.get(i));
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
            final InferencePartialPayload payload = createInput(ids.get(i));
            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .body(is(""));
        }
        for (int i = 0; i < 5; i++) {
            final InferencePartialPayload payload = createOutput(ids.get(i));
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