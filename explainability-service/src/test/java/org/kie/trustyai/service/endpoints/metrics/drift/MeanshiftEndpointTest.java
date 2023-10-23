package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.drift.meanshift.MeanshiftFitting;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(MeanshiftEndpoint.class)
class MeanshiftEndpointTest {

    private static final String MODEL_ID = "example1";
    private static final String TRAINING_TAG = "TRAINING";
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
        Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(datasource.get().generateRandomDataframeDrifted(N_SAMPLES).asPredictions());
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        scheduler.get().getAllRequests().clear();
    }

    @Test
    void meanshiftNonPreFit() {
        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);

        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals(0, response.getNamedValues().get("age"));
        assertEquals(.570004, response.getNamedValues().get("race"), 1e-5);
        assertEquals(1, response.getNamedValues().get("income"));
    }

    @Test
    void meanshiftPreFit() {
        Dataframe dfTrain = datasource.get().getDataframe(MODEL_ID).filterRowsByTagEquals(TRAINING_TAG);
        MeanshiftFitting msf = Meanshift.precompute(dfTrain);

        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        payload.setFitting(msf.getFitStats());

        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals(0, response.getNamedValues().get("age"));
        assertEquals(.570004, response.getNamedValues().get("race"), 1e-5);
        assertEquals(1, response.getNamedValues().get("income"));
    }

    @Test
    void meanshiftNonPreFitRequest() throws InterruptedException {
        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
