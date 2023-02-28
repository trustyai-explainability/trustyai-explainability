package org.kie.trustyai.service.scenarios.nodata;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.PayloadProducer;
import org.kie.trustyai.service.endpoints.consumer.ConsumerEndpoint;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

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
class ConsumerEndpointTest {

    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    @Test
    void consumePostCorrect() {
        final InferencePayload payload = PayloadProducer.getInferencePayload(0);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)

                .body(is(""));

        final Dataframe dataframe = datasource.get().getDataframe();
        assertEquals(1, dataframe.getRowDimension());
        assertEquals(4, dataframe.getColumnDimension());
        assertEquals(3, dataframe.getInputsCount());
        assertEquals(1, dataframe.getOutputsCount());
    }

    @Test
    void consumePostIncorrect() {
        final InferencePayload payload = PayloadProducer.getInferencePayload(1);
        // Mangle inputs
        payload.setInput(payload.getInput().substring(0, 10) + "X" + payload.getInput().substring(11));
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.INTERNAL_SERVER_ERROR)

                .body(is(""));
    }

}