package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.utils.PerColumnStatistics;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;
import org.kie.trustyai.service.payloads.service.NameMapping;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.kie.trustyai.service.utils.DataframeGenerators;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(MeanshiftEndpoint.class)
abstract class MeanshiftEndpointBaseTest {

    protected static final String MODEL_ID = "example1";
    protected static final String TRAINING_TAG = "TRAINING";
    protected static final int N_SAMPLES = 100;
    @Inject
    Instance<DataSource> datasource;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @BeforeEach
    void populateStorage() {
        clearData();
        saveDF(getTaggedDataframe());
    }

    abstract void clearData();

    abstract void saveDF(Dataframe dataframe);

    public Dataframe getTaggedDataframe(){
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(DataframeGenerators.generateRandomDataframeDrifted(N_SAMPLES).asPredictions());
        return dataframe;
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
                .when().post().peek()
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
        PerColumnStatistics msf = Meanshift.precompute(dfTrain);

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

    @Test
    void meanshiftNameMappedNonPreFitRequest() throws InterruptedException {
        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.put("age", "Age Mapped");
        inputMapping.put("gender", "Gender Mapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .basePath("/info")
                .when().post("/names")
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post().peek()
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void meanshiftNameMappedNonPreFitRequestNoInferenceData() throws InterruptedException {
        clearData();

        // add only TRAINING data, no inference data
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);
        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        saveDF(dataframe);

        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.put("age", "Age Mapped");
        inputMapping.put("gender", "Gender Mapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .basePath("/info")
                .when().post("/names")
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request").peek()
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
