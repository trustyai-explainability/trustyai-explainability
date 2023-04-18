package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceRequestsEndpointTest {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @Inject
    Instance<ServiceConfig> serviceConfig;

    @BeforeEach
    void populateStorage() {
        // Empty mock storage
        storage.get().emptyStorage();
        // Clear any requests between tests
        scheduler.get().getDirRequests().clear();
        scheduler.get().getSpdRequests().clear();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    /**
     * When no batch size is specified in the request, the service's default batch size should be used
     */
    @Test
    void postCorrectRequestDefaultBatchSize() {

        final BaseMetricRequest payload = RequestPayloadGenerator.correct();

        final BaseScheduledResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseScheduledResponse.class);

        // Get stored request
        final BaseMetricRequest request = scheduler
                .get()
                .getSpdRequests()
                .get(response.getRequestId());

        final int defaultBatchSize = serviceConfig.get().batchSize().getAsInt();
        assertEquals(defaultBatchSize, request.getBatchSize());
    }

    /**
     * When a batch size is specified in the request, that value should be used
     */
    @Test
    void postCorrectRequestBatchSize() {

        final int BATCH_SIZE = 1000;

        final BaseMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setBatchSize(BATCH_SIZE);

        final BaseScheduledResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseScheduledResponse.class);

        // Get stored request
        final BaseMetricRequest request = scheduler
                .get()
                .getSpdRequests()
                .get(response.getRequestId());

        assertEquals(BATCH_SIZE, request.getBatchSize());
    }

    /**
     * When an invalid batch size is specified in the request, an error should be thrown
     */
    @Test
    void postIncorrectRequestBatchSize() {

        final int BATCH_SIZE = -1;

        final BaseMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setBatchSize(BATCH_SIZE);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("Request batch size must be bigger than 0."));

        // Get stored request
        final Map<UUID, BaseMetricRequest> requests = scheduler
                .get()
                .getDirRequests();

        assertTrue(requests.isEmpty());
    }

}