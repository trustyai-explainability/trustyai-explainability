package org.kie.trustyai.service.scenarios.nodata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.endpoints.metrics.DisparateImpactRatioEndpoint;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
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

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointTest {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    /**
     * Populate storage with 1000 random observations before each test.
     *
     */
    @BeforeEach
    void populateStorageEmpty() {
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
    void dirPostCorrect() {
        final BaseMetricRequest payload = RequestPayloadGenerator.correct();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("No data available"));

    }

    @Test
    void dirPostIncorrectType() {
        final BaseMetricRequest payload = RequestPayloadGenerator.incorrectType();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("No data available"));
    }

    @Test
    void dirPostIncorrectInput() {
        final Map<String, Object> payload = RequestPayloadGenerator.incorrectInput();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("No data available"));

    }

    @Test
    void listSchedules() {

        // No schedule request made yet
        final ScheduleList emptyList = given()
                .when()
                .get("/requests")
                .then().statusCode(RestResponse.StatusCode.OK).extract().body().as(ScheduleList.class);

        assertEquals(0, emptyList.requests.size());

        // Perform multiple schedule requests
        final BaseMetricRequest payload = RequestPayloadGenerator.correct();
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

    void populateStorage() {
        populateStorageEmpty();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @Test
    void listNames() {
        populateStorage();

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
            final BaseMetricRequest payload = RequestPayloadGenerator.named(name);
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

}
