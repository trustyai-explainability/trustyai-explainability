package org.kie.trustyai.service.endpoints.consumer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.reconcilers.ModelMeshInferencePayloadReconciler;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

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

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointTest {

    @Inject
    Instance<MockCSVDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    ModelMeshInferencePayloadReconciler reconciler;

    /**
     * Empty the storage before each test.
     */
    @BeforeEach
    void emptyStorage() throws JsonProcessingException {
        datasource.get().reset();
        storage.get().emptyStorage();
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
        assertEquals("Error reading dataframe for model=" + MODEL_A_ID + ": Data file '" + MODEL_A_ID + "-data.csv' not found", exception.getMessage());

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
        assertEquals("Error reading dataframe for model=" + MODEL_A_ID + ": Data file '" + MODEL_A_ID + "-data.csv' not found", exception.getMessage());

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
}
