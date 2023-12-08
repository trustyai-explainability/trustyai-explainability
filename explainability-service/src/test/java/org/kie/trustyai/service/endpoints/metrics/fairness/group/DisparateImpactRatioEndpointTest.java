package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionMetadata;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointTest {

    private static final String MODEL_ID = "example1";
    private static final int N_SAMPLES = 100;
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        scheduler.get().getAllRequests().clear();
    }

    @Test
    void get() {
        when().get()
                .then()
                .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode())
                .body(is(""));
    }

    @Test
    void postCorrect() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.correct();

        final BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals("metric", response.getType());
        assertEquals("DIR", response.getName());
        assertFalse(Double.isNaN(response.getValue()));
    }

    @Test
    void postMultiValueCorrect() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.multiValueCorrect();

        final BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then().statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals("metric", response.getType());
        assertEquals("DIR", response.getName());
        assertFalse(Double.isNaN(response.getValue()));
    }

    @Test
    void postMultiValueMismatchingType() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.multiValueMismatchingType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("Received invalid type for privileged attribute=age: got 'wrong', expected object compatible with 'INT32'"));
    }

    @Test
    void postThresh() throws JsonProcessingException {
        datasource.get().reset();

        // with large threshold, the DIR is inside bounds
        GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setThresholdDelta(.5);
        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals("metric", response.getType());
        assertEquals("DIR", response.getName());
        assertFalse(response.getThresholds().outsideBounds);

        // with negative threshold, the DIR is guaranteed outside bounds
        payload = RequestPayloadGenerator.correct();
        payload.setThresholdDelta(-.5);
        response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals("metric", response.getType());
        assertEquals("DIR", response.getName());
        assertTrue(response.getThresholds().outsideBounds);
    }

    @Test
    @DisplayName("DIR request incorrectly typed")
    void postIncorrectType() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("Invalid type for output=income: got 'male', expected object compatible with 'INT32'"));
    }

    @Test
    @DisplayName("DIR request with incorrect input")
    void postIncorrectInput() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectInput();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("No protected attribute found with name=city"));

    }

    @Test
    void postUnknownType() throws JsonProcessingException {
        datasource.get().reset();

        final Map<String, Object> payload = RequestPayloadGenerator.unknownType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(any(String.class));

    }

    @Test
    void postManyWrongNames() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectManyWrongNames();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(allOf(
                        containsString("No output found with name=icnome"),
                        containsString("No protected attribute found with name=city")));
    }

    @Test
    void postManyWrongTypes() throws JsonProcessingException {
        datasource.get().reset();

        final GroupMetricRequest payload = RequestPayloadGenerator.incorrectManyWrongTypes();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(allOf(
                        containsString("Invalid type for output=income: got 'approved-doesnt-exist', expected object compatible with 'INT32'"),
                        containsString("Received invalid type for privileged attribute=gender: got 'lemons', expected object compatible with 'INT32'"),
                        containsString("Received invalid type for unprivileged attribute=gender: got '1.5', expected object compatible with 'INT32'")));
    }

    @Test
    void listSchedules() throws JsonProcessingException {
        datasource.get().reset();

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(firstRequest.getRequestId());

        final BaseScheduledResponse secondRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(secondRequest.getRequestId());

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(2, scheduleList.requests.size());

        // Remove one request
        final ScheduleId firstRequestId = new ScheduleId();
        firstRequestId.requestId = firstRequest.getRequestId();
        given().contentType(ContentType.JSON).when().body(firstRequestId).delete("/request")
                .then().statusCode(Response.Status.OK.getStatusCode()).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(1, scheduleList.requests.size());

        // Remove second request
        final ScheduleId secondRequestId = new ScheduleId();
        secondRequestId.requestId = secondRequest.getRequestId();
        given().contentType(ContentType.JSON).when().body(secondRequestId).delete("/request")
                .then().statusCode(Response.Status.OK.getStatusCode()).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());

        // Remove non-existing request
        final ScheduleId nonExistingRequestId = new ScheduleId();
        nonExistingRequestId.requestId = secondRequest.getRequestId();
        given().contentType(ContentType.JSON).when().body(nonExistingRequestId).delete("/request")
                .then().statusCode(RestResponse.StatusCode.NOT_FOUND).body(is(""));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());
    }

    @Test
    void requestWrongType() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then()
                .statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(firstRequest.getRequestId());

        final GroupMetricRequest wrongPayload = RequestPayloadGenerator.incorrectType();
        given()
                .contentType(ContentType.JSON)
                .body(wrongPayload)
                .when()
                .post("/request")
                .then().statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("got 'male', expected object compatible with 'INT32'"));

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(1, scheduleList.requests.size());

        // Remove one request
        final ScheduleId firstRequestId = new ScheduleId();
        firstRequestId.requestId = firstRequest.getRequestId();
        given().contentType(ContentType.JSON).when().body(firstRequestId).delete("/request")
                .then().statusCode(200).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());

    }

    @Test
    void requestUnknowType() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse firstRequest = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/request")
                .then().statusCode(200).extract().body().as(BaseScheduledResponse.class);

        assertNotNull(firstRequest.getRequestId());

        final Map<String, Object> unknownPayload = RequestPayloadGenerator.unknownType();
        given()
                .contentType(ContentType.JSON)
                .body(unknownPayload)
                .when()
                .post("/request")
                .then().statusCode(RestResponse.StatusCode.BAD_REQUEST);

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(1, scheduleList.requests.size());

        // Remove one request
        final ScheduleId firstRequestId = new ScheduleId();
        firstRequestId.requestId = firstRequest.getRequestId();
        given().contentType(ContentType.JSON).when().body(firstRequestId).delete("/request")
                .then().statusCode(200).body(is("Removed"));

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(200).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());

    }

    @Test
    void listNames() {
        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);
        assertEquals(0, emptyList.requests.size());

        List<String> names = List.of("name1", "name2", "name3");
        Map<UUID, String> nameIDs = new HashMap<>();

        // Perform multiple schedule requests
        for (String name : names) {
            final GroupMetricRequest payload = RequestPayloadGenerator.named(name);
            BaseScheduledResponse scheduledResponse = given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/request")
                    .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(BaseScheduledResponse.class);
            nameIDs.put(scheduledResponse.getRequestId(), name);
        }

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(3, scheduleList.requests.size());

        // check that names are as expected
        for (int i = 0; i < scheduleList.requests.size(); i++) {
            UUID returnedID = scheduleList.requests.get(i).id;
            assertEquals(nameIDs.get(returnedID), scheduleList.requests.get(i).request.getRequestName());

            // delete the corresponding request
            final ScheduleId thisRequestId = new ScheduleId();
            thisRequestId.requestId = returnedID;
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .body(thisRequestId)
                    .delete("/request")
                    .then()
                    .statusCode(200)
                    .body(is("Removed"));
        }

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());
    }

    @Test
    void listThresholds() {
        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);
        assertEquals(0, emptyList.requests.size());

        List<Double> threshs = List.of(.1, .05, .5);
        Map<UUID, Double> threshIDs = new HashMap<>();

        // Perform multiple schedule requests
        for (Double thresh : threshs) {
            final GroupMetricRequest payload = RequestPayloadGenerator.correct();
            payload.setThresholdDelta(thresh);
            BaseScheduledResponse scheduledResponse = given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/request")
                    .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(BaseScheduledResponse.class);
            threshIDs.put(scheduledResponse.getRequestId(), thresh);
        }

        ScheduleList scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(3, scheduleList.requests.size());

        // check that names are as expected
        for (int i = 0; i < scheduleList.requests.size(); i++) {
            UUID returnedID = scheduleList.requests.get(i).id;
            assertEquals(threshIDs.get(returnedID), ((GroupMetricRequest) scheduleList.requests.get(i).request).getThresholdDelta());

            // delete the corresponding request
            final ScheduleId thisRequestId = new ScheduleId();
            thisRequestId.requestId = returnedID;
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .body(thisRequestId)
                    .delete("/request")
                    .then()
                    .statusCode(200)
                    .body(is("Removed"));
        }

        scheduleList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        // Correct number of active requests
        assertEquals(0, scheduleList.requests.size());
    }

    @Test
    void postCorrectFilteringSynthetic() throws JsonProcessingException {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        Double value = response.getValue();
        assertEquals("metric", response.getType());
        assertEquals("DIR", response.getName());
        assertFalse(Double.isNaN(value));

        final Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        Prediction prediction = dataframe.asPredictions().get(0);
        PredictionMetadata predictionMetadata = new PredictionMetadata("123", LocalDateTime.now(), Dataframe.InternalTags.SYNTHETIC.get());
        Prediction newPrediction = new SimplePrediction(prediction.getInput(), prediction.getOutput(), predictionMetadata);
        Dataframe newDataframe = Dataframe.createFrom(newPrediction);

        datasource.get().saveDataframe(newDataframe, MODEL_ID);

        final BaseMetricResponse responseSecond = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals("metric", responseSecond.getType());
        assertEquals("DIR", responseSecond.getName());
        assertEquals(value, responseSecond.getValue());
    }
}
