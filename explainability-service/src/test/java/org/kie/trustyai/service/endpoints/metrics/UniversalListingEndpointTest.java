package org.kie.trustyai.service.endpoints.metrics;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
class UniversalListingEndpointTest {
    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage();
        scheduler.get().getAllRequests().clear();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clean() {
        scheduler.get().getAllRequests().clear();
    }

    @DisplayName("Check multi-metrics requests are returned")
    @Test
    void requestMultipleMetricsSize() {
        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/metrics/all/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/dir/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(firstRequest.getRequestId());
        final UUID first = firstRequest.getRequestId();

        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/spd/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(secondRequest.getRequestId());
        final UUID second = secondRequest.getRequestId();

        // Create an identity metric request
        final BaseScheduledResponse thirdRequest = given()
                .contentType(ContentType.JSON)
                .body(RequestPayloadGenerator.identityCorrect())
                .when()
                .post("/metrics/identity/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(thirdRequest.getRequestId());
        final UUID third = thirdRequest.getRequestId();


        ScheduleList scheduleList = given()
                .when()
                .get("/metrics/all/requests").peek()
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        for (ScheduleRequest sr : scheduleList.requests) {
            if (sr.id.equals(first)) {
                assertEquals("DIR", sr.request.getMetricName());
                GroupMetricRequest gmr = (GroupMetricRequest) sr.request;
                assertFalse(gmr.privilegedAttribute.isMultipleValued());
            } else if (sr.id.equals(second)) {
                assertEquals("SPD", sr.request.getMetricName());
            }  else if (sr.id.equals(third)) {
                assertEquals("IDENTITY", sr.request.getMetricName());
            }

            else {
                fail();
            }
        }

        // ?type=all has the same effect as not query parameter
        scheduleList = given()
                .when()
                .get("/metrics/all/requests?type=all").peek()
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        for (ScheduleRequest sr : scheduleList.requests) {
            if (sr.id.equals(first)) {
                assertEquals("DIR", sr.request.getMetricName());
                GroupMetricRequest gmr = (GroupMetricRequest) sr.request;
                assertFalse(gmr.privilegedAttribute.isMultipleValued());
            } else if (sr.id.equals(second)) {
                assertEquals("SPD", sr.request.getMetricName());
            }  else if (sr.id.equals(third)) {
                assertEquals("IDENTITY", sr.request.getMetricName());
            }

            else {
                fail();
            }
        }

        // Correct number of active requests
        assertEquals(3, scheduleList.requests.size());
    }

    @DisplayName("Check multi-metrics requests are returned for fairness type")
    @Test
    void requestMultipleMetricsFairnessSize() {
        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/metrics/all/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/dir/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(firstRequest.getRequestId());
        final UUID first = firstRequest.getRequestId();

        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/spd/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(secondRequest.getRequestId());
        final UUID second = secondRequest.getRequestId();

        // Create an identity metric request
        final BaseScheduledResponse thirdRequest = given()
                .contentType(ContentType.JSON)
                .body(RequestPayloadGenerator.identityCorrect())
                .when()
                .post("/metrics/identity/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(thirdRequest.getRequestId());
        final UUID third = thirdRequest.getRequestId();


        ScheduleList scheduleList = given()
                .when()
                .get("/metrics/all/requests?type=fairness").peek()
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        for (ScheduleRequest sr : scheduleList.requests) {
            if (sr.id.equals(first)) {
                assertEquals("DIR", sr.request.getMetricName());
                GroupMetricRequest gmr = (GroupMetricRequest) sr.request;
                assertFalse(gmr.privilegedAttribute.isMultipleValued());
            } else if (sr.id.equals(second)) {
                assertEquals("SPD", sr.request.getMetricName());
            } else {
                fail();
            }
        }

        // Correct number of active requests
        assertEquals(2, scheduleList.requests.size());
    }

    @DisplayName("Check multi-metrics requests with incorrect type")
    @Test
    void requestMultipleMetricsIncorrectType() {
        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/metrics/all/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);
        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/dir/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(firstRequest.getRequestId());

        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/metrics/spd/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(secondRequest.getRequestId());

        // Create an identity metric request
        final BaseScheduledResponse thirdRequest = given()
                .contentType(ContentType.JSON)
                .body(RequestPayloadGenerator.identityCorrect())
                .when()
                .post("/metrics/identity/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);
        assertNotNull(thirdRequest.getRequestId());

        given()
                .when()
                .get("/metrics/all/requests?type=foo").peek()
                .then().statusCode(400);

    }

}
