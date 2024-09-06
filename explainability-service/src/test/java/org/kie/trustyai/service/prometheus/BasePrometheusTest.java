package org.kie.trustyai.service.prometheus;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;

import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BasePrometheusTest {
    protected static final String MODEL_ID = "example1";
    protected static final String TRAINING_TAG = "TRAINING";
    protected static final int N_SAMPLES = 100;

    @Inject
    Instance<DataSource> datasource;

    public Dataframe getTaggedDataframe() {
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(DataframeGenerators.generateRandomDataframeDrifted(N_SAMPLES).asPredictions());
        return dataframe;
    }


    @BeforeEach
    void populateStorage() {
        // Empty mock storage
        cleanStorage();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        saveDF(dataframe, MODEL_ID);
    }

    abstract void cleanStorage();

    void saveDF(Dataframe dataframe, String modelId){
        datasource.get().saveDataframe(dataframe, modelId);
        datasource.get().saveMetadata(MockCSVDatasource.createMetadata(dataframe), modelId);
    }


    Pair<String, String> createThenDeleteRequest(String endpoint, Object payload) throws InterruptedException {
        final BaseScheduledResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post(endpoint)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseScheduledResponse.class);

        final ScheduleId requestId = new ScheduleId();
        requestId.requestId = response.getRequestId();

        // wait for metrics to publish
        for (int i = 0; i < 2; i++) {
            Thread.sleep(1000);
        }

        // grab metrics endpoint before deletion
        String metricsListBeforeDelete = given()
                .when()
                .basePath("/q/metrics")
                .get().then().statusCode(Response.Status.OK.getStatusCode())
                .extract().body()
                .asString();

        // delete metric request
        given()
                .contentType(ContentType.JSON)
                .body(requestId)
                .when().delete(endpoint)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        // grab metrics endpoint after deletion
        String metricsListAfterDelete = given()
                .when()
                .basePath("/q/metrics")
                .get().then().statusCode(Response.Status.OK.getStatusCode())
                .extract().body()
                .asString();

        return Pair.of(metricsListBeforeDelete, metricsListAfterDelete);

    }

    /**
     * Deleting a request should remove it from the Prometheus /q/metrics endpoint
     */
    @Test
    @DisplayName("Deleted single-valued requests are removed from Prometheus metrics endpoint")
    void deleteFairnessRequest() throws InterruptedException {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        Pair<String, String> metricsRequests = createThenDeleteRequest("/metrics/group/fairness/spd/request", payload);


        // before deletion, the metrics should exist in the metrics endpoint
        assertTrue(metricsRequests.getLeft()
                .contains("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\""));

        // after deletion, they should not
        assertFalse(metricsRequests.getRight()
                .contains("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\""));
    }


    @Test
    @DisplayName("Deleted multi-valued requests are removed from Prometheus metrics endpoint")
    void deleteDriftRequest() throws InterruptedException {
        Dataframe taggedDataframe = getTaggedDataframe();
        saveDF(taggedDataframe, MODEL_ID);

        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        Pair<String, String> metricsRequests = createThenDeleteRequest("/metrics/drift/meanshift/request", payload);

        for (String column : taggedDataframe.getInputNames()) {
            // before deletion, the metrics should exist in the metrics endpoint
            assertTrue(metricsRequests.getLeft().contains("trustyai_meanshift{batch_size=\"5000\",metricName=\"MEANSHIFT\""));
            assertTrue(metricsRequests.getLeft().contains("subcategory=\"" + column + "\""));

            // after deletion, they should not
            assertFalse(metricsRequests.getRight().contains("trustyai_meanshift{batch_size=\"5000\",metricName=\"MEANSHIFT\""));
            assertFalse(metricsRequests.getRight().contains("subcategory=\"" + column + "\""));
        }
    }
}
