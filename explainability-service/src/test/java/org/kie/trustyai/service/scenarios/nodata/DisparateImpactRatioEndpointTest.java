package org.kie.trustyai.service.scenarios.nodata;

import java.util.Map;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.endpoints.metrics.DisparateImpactRatioEndpoint;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(NoDataTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointTest {

    @Test
    void dirGet() {
        when().get()
                .then()
                .statusCode(405)
                .body(is(""));
    }

    @Test
    void dirPostCorrect() {
        final Map<String, Object> payload = RequestPayloadGenerator.correct();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.INTERNAL_SERVER_ERROR)
                .body(is(""));

    }

    @Test
    void dirPostIncorrectType() {
        final Map<String, Object> payload = RequestPayloadGenerator.incorrectType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.INTERNAL_SERVER_ERROR)
                .body(is(""));
    }

    @Test
    void dirPostIncorrectInput() {
        final Map<String, Object> payload = RequestPayloadGenerator.incorrectInput();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(500)
                .body(is(""));

    }

    @Test
    void listSchedules() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final Map<String, Object> payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(firstRequest.requestId);

        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(secondRequest.requestId);

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(2, scheduleList.requests.size());

        // Remove one request
        final ScheduleId firstRequestId = new ScheduleId();
        firstRequestId.requestId = firstRequest.requestId;
        given().contentType(ContentType.JSON).when().body(firstRequestId).delete("/request")
                .then().statusCode(200).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(1, scheduleList.requests.size());

        // Remove second request
        final ScheduleId secondRequestId = new ScheduleId();
        secondRequestId.requestId = secondRequest.requestId;
        given().contentType(ContentType.JSON).when().body(secondRequestId).delete("/request")
                .then().statusCode(200).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());

        // Remove non-existing request
        final ScheduleId nonExistingRequestId = new ScheduleId();
        nonExistingRequestId.requestId = secondRequest.requestId;
        given().contentType(ContentType.JSON).when().body(nonExistingRequestId).delete("/request")
                .then().statusCode(RestResponse.StatusCode.NOT_FOUND).body(is(""));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());
    }

}