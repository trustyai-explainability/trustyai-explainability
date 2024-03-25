package org.kie.trustyai.service.scenarios.nodata;

import java.util.UUID;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.DisparateImpactRatioEndpoint;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointTest {

    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    /**
     * Populate storage with 1000 random observations before each test.
     *
     */
    @BeforeEach
    void populateStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void dirGet() {
        when().get()
                .then()
                .statusCode(RestResponse.StatusCode.METHOD_NOT_ALLOWED)
                .body(is(""));
    }

    @Test
    @DisplayName("DIR POST correct (no data)")
    void dirPostCorrect() {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No metadata found for model=" + payload.getModelId()));
    }

    @Test
    void dirPostIncorrectType() {
        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No metadata found for model=" + payload.getModelId()));
    }

    @Test
    void dirPostIncorrectInput() {
        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectInput();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No metadata found for model=" + payload.getModelId()));

    }

    void listSchedules() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(RestResponse.StatusCode.BAD_REQUEST).body(is("No metadata found for model=" + payload.getModelId()));

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());

        // Remove non-existing request
        final ScheduleId nonExistingRequestId = new ScheduleId();
        nonExistingRequestId.requestId = UUID.randomUUID();
        given().contentType(ContentType.JSON).when().body(nonExistingRequestId).delete("/request")
                .then().statusCode(RestResponse.StatusCode.NOT_FOUND).body(is(""));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());
    }
}
