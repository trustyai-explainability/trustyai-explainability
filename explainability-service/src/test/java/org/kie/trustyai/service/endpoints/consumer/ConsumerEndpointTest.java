package org.kie.trustyai.service.endpoints.consumer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.*;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.trustyai.service.PayloadProducer.MODEL_A_ID;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    @Order(1)
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

    @Order(3)
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

    @Order(2)
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

    @Order(4)
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
        final UUID id = UUID.randomUUID();
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
        assertEquals("Data file not found", exception.getMessage());

    }

    @Test
    void consumePartialPostOutputsOnly() {
        final UUID id = UUID.randomUUID();
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
        assertEquals("Data file not found", exception.getMessage());

    }

    @Test
    void consumePartialPostSome() {
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
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
        final List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
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

}