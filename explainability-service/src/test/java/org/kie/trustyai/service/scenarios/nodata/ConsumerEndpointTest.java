package org.kie.trustyai.service.scenarios.nodata;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.endpoints.consumer.ConsumerEndpoint;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(NoDataTestProfile.class)
@TestHTTPEndpoint(ConsumerEndpoint.class)
class ConsumerEndpointTest {

    @Inject
    Instance<MockConsumerDatasource> datasource;

    @Test
    void consumePostCorrect() {
        final InferencePayload payload = PayloadProducer.getInferencePayload(0);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(200)

                .body(is(""));

        final Dataframe dataframe = datasource.get().getCurrent();
        assertEquals(1, dataframe.getRowDimension());
        assertEquals(4, dataframe.getColumnDimension());
        assertEquals(3, dataframe.getInputsCount());
        assertEquals(1, dataframe.getOutputsCount());
    }

    @Test
    void consumePostIncorrect() {
        final InferencePayload payload = PayloadProducer.getInferencePayload(1);
        // Mangle inputs
        payload.input = payload.input.substring(0, 10) + "X" + payload.input.substring(11);
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(500)

                .body(is(""));
    }

}