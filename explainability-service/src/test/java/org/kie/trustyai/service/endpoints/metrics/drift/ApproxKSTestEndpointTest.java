package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.kstest.ApproxKSFitting;
import org.kie.trustyai.metrics.drift.kstest.ApproxKSTest;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.kstest.ApproxKSTestMetricRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(ApproxKSTestEndpoint.class)
class ApproxKSTestEndpointTest {

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
        Dataframe dataframe = datasource.get().generateDataframeFromNormalDistributions(N_SAMPLES, 1.0, 2.0);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(datasource.get().generateDataframeFromNormalDistributions(N_SAMPLES, 2.0, 1.0).asPredictions());
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        scheduler.get().getAllRequests().clear();
    }

    @Test
    void approxKSTestNonPreFit() {
        ApproxKSTestMetricRequest payload = new ApproxKSTestMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        payload.setEpsilon(0.005);

        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);
        assertTrue(response.getNamedValues().get("f1") < 0.05);
        assertTrue(response.getNamedValues().get("f2") < 0.05);
        assertTrue(response.getNamedValues().get("f3") < 0.05);
        assertTrue(response.getNamedValues().get("income") > 0.05);

    }

    @Test
    void approxKSTestPreFit() {
        Dataframe dfTrain = datasource.get().getDataframe(MODEL_ID).filterRowsByTagEquals(TRAINING_TAG);
        ApproxKSFitting aksf = ApproxKSTest.precompute(dfTrain, 0.01);

        ApproxKSTestMetricRequest payload = new ApproxKSTestMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        payload.setEpsilon(0.005);
        payload.setSketchFitting(aksf.getfitSketches());

        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertTrue(response.getNamedValues().get("f1") < 0.05);
        assertTrue(response.getNamedValues().get("f2") < 0.05);
        assertTrue(response.getNamedValues().get("f3") < 0.05);
        assertTrue(response.getNamedValues().get("income") > 0.05);
    }

    @Test
    void approxKSTestNonPreFitRequest() throws InterruptedException {
        ApproxKSTestMetricRequest payload = new ApproxKSTestMetricRequest();
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