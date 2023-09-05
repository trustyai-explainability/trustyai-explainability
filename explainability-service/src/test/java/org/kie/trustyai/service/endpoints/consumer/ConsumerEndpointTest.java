package org.kie.trustyai.service.endpoints.consumer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.DatapointSource;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;
import org.kie.trustyai.service.payloads.consumer.upload.ModelInferJointPayload;
import org.kie.trustyai.service.utils.KserveRestPayloads;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.trustyai.service.PayloadProducer.MODEL_A_ID;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointTest {

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

    @Test
    void consumeFullPostCorrectModelA() {
        final InferencePayload payload = PayloadProducer.getInferencePayloadA(0);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/full")
                .then()
                .statusCode(RestResponse.StatusCode.OK)

                .body(is(""));

        final Dataframe dataframe = datasource.get().getDataframe(payload.getModelId());
        assertEquals(1, dataframe.getRowDimension());
        assertEquals(4, dataframe.getColumnDimension());
        assertEquals(3, dataframe.getInputsCount());
        assertEquals(1, dataframe.getOutputsCount());
    }

    @Test
    void consumeFullPostIncorrectModelA() {
        final InferencePayload payload = PayloadProducer.getInferencePayloadA(1);
        // Mangle inputs
        payload.setInput(payload.getInput().substring(0, 10) + "X" + payload.getInput().substring(11));
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/full")
                .then()
                .statusCode(RestResponse.StatusCode.INTERNAL_SERVER_ERROR)

                .body(is(""));
    }

    @Test
    void consumeFullPostCorrectModelB() {
        final InferencePayload payload = PayloadProducer.getInferencePayloadB(1);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/full")
                .then()
                .statusCode(RestResponse.StatusCode.OK)

                .body(is(""));

        final Dataframe dataframe = datasource.get().getDataframe(PayloadProducer.MODEL_B_ID);
        assertEquals(1, dataframe.getRowDimension());
        assertEquals(5, dataframe.getColumnDimension());
        assertEquals(3, dataframe.getInputsCount());
        assertEquals(2, dataframe.getOutputsCount());
    }

    @Test
    void consumeFullPostIncorrectModelB() {
        final InferencePayload payload = PayloadProducer.getInferencePayloadA(1);
        // Mangle inputs
        payload.setInput(payload.getInput().substring(0, 10) + "X" + payload.getInput().substring(11));
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/full")
                .then()
                .statusCode(RestResponse.StatusCode.INTERNAL_SERVER_ERROR)

                .body(is(""));
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
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("Invalid schema for payload request id=" + newId + ", Payload schema and stored schema are not the same"));

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

    // data upload tests ===============================================================================================
    @Test
    void uploadData() {
        int[] testInputRows = new int[] { 1, 5, 250 };
        int[] testInputCols = new int[] { 1, 4 };
        int[] testOutputCols = new int[] { 1, 2 };
        String[] testDatatypes = new String[] { "INT64", "INT32", "FP32", "FP64", "BOOL" };

        // sorry for the quad loop
        for (int nInputRows : testInputRows) {
            for (int nInputCols : testInputCols) {
                for (int nOutputCols : testOutputCols) {
                    for (String datatype : testDatatypes) {
                        ModelInferJointPayload payload = KserveRestPayloads.generatePayload(nInputRows, nInputCols, nOutputCols, datatype, "TRAINING");
                        emptyStorage();

                        given()
                                .contentType(ContentType.JSON)
                                .body(payload)
                                .when().post("/upload")
                                .then()
                                .statusCode(RestResponse.StatusCode.OK)
                                .body(containsString(nInputRows + " datapoints"));

                        // check that tagging is correctly applied
                        Dataframe df = datasource.get().getDataframe(payload.getModelName());
                        Dataframe trainDF = df.filterRowsByTagEquals(DatapointSource.TRAINING);
                        Dataframe nonTrainDF = df.filterRowsByTagNotEquals(DatapointSource.TRAINING);

                        assertEquals(nInputRows, df.getRowDimension());
                        assertEquals(nInputRows, trainDF.getRowDimension());
                        assertEquals(0, nonTrainDF.getRowDimension());
                    }
                }
            }
        }
    }

    @Test
    void uploadMultipleTagging() {
        int nPayload1 = 50;
        int nPayload2 = 51;
        ModelInferJointPayload payload1 = KserveRestPayloads.generatePayload(nPayload1, 10, 1, "INT64", "TRAINING");
        ModelInferJointPayload payload2 = KserveRestPayloads.generatePayload(nPayload2, 10, 1, "INT64", "SYNTHETIC");

        given()
                .contentType(ContentType.JSON)
                .body(payload1)
                .when().post("/upload")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(containsString(nPayload1 + " datapoints"));

        given()
                .contentType(ContentType.JSON)
                .body(payload2)
                .when().post("/upload")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(containsString(nPayload2 + " datapoints"));

        // check that tagging is correctly applied
        Dataframe df = datasource.get().getDataframe(payload1.getModelName());
        Dataframe trainDF = df.filterRowsByTagEquals(DatapointSource.TRAINING);
        Dataframe synthDF = df.filterRowsByTagEquals(DatapointSource.SYNTHETIC);

        assertEquals(nPayload1 + nPayload2, df.getRowDimension());
        assertEquals(nPayload1, trainDF.getRowDimension());
        assertEquals(nPayload2, synthDF.getRowDimension());
    }

    @Test
    void uploadTagThatDoesntExist() {
        ModelInferJointPayload payload1 = KserveRestPayloads.generatePayload(5, 10, 1, "INT64", "enumthatdoesntexist");

        given()
                .contentType(ContentType.JSON)
                .body(payload1)
                .when().post("/upload")
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("Provided datapoint tag=enumthatdoesntexist is not valid. Must be one of [SYNTHETIC, TRAINING, UNLABELED]"));
    }

}
