package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioRequestsEndpointTest {

    private static final int N_SAMPLES = 100;
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
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage();
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
                .getDirRequests()
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
                .getDirRequests()
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

    @DisplayName("DIR request with custom batch size")
    void requestCustomBatchSize() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final BaseMetricRequest payload = RequestPayloadGenerator.correct();
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