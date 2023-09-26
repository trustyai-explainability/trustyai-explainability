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
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDFitting;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(FourierMMDEndpoint.class)
class FourierMMDEndpointTest {

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
        FourierMMDMetricRequest payload = new FourierMMDMetricRequest();
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
    void fourierMMDPreFit() {
        Dataframe dfTrain = datasource.get().getDataframe(MODEL_ID).filterRowsByTagEquals(TRAINING_TAG);
        FourierMMDFitting msf = FourierMMD.precompute(dfTrain);

        FourierMMDMetricRequest payload = new FourierMMDMetricRequest();
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
    void fourierMMDNonPreFitRequest() throws InterruptedException {
        FourierMMDMetricRequest payload = new FourierMMDMetricRequest();
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
