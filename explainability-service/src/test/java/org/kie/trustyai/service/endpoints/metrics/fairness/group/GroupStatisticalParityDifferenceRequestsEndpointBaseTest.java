package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.Map;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

abstract class GroupStatisticalParityDifferenceRequestsEndpointBaseTest {

    protected static final String MODEL_ID = "example1";
    protected static final int N_SAMPLES = 100;
    @Inject
    Instance<MockDatasource> datasource;


    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @Inject
    Instance<ServiceConfig> serviceConfig;

    /**
     * When no batch size is specified in the request, the service's default batch size should be used
     */
    @Test
    void postCorrectRequestDefaultBatchSize() {

        final GroupMetricRequest payload = RequestPayloadGenerator.correct();

        final BaseScheduledResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseScheduledResponse.class);

        // Get stored request
        final GroupMetricRequest request = (GroupMetricRequest) scheduler
                .get()
                .getRequests("SPD")
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

        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
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
        final GroupMetricRequest request = (GroupMetricRequest) scheduler
                .get()
                .getRequests("SPD")
                .get(response.getRequestId());

        assertEquals(BATCH_SIZE, request.getBatchSize());
    }

    /**
     * When an invalid batch size is specified in the request, an error should be thrown
     */
    @Test
    void postIncorrectRequestBatchSize() {

        final int BATCH_SIZE = -1;

        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
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
                .getRequests("SPD");

        assertTrue(requests.isEmpty());
    }

    @DisplayName("DIR request with custom batch size")
    void requestCustomBatchSize() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setBatchSize(N_SAMPLES - 10);
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(firstRequest.getRequestId());

        payload.setBatchSize(N_SAMPLES + 1000);
        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(secondRequest.getRequestId());

        payload.setBatchSize(0);
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(400).body(containsString("Request batch size must be bigger than 0."));

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(2, scheduleList.requests.size());
    }

}
