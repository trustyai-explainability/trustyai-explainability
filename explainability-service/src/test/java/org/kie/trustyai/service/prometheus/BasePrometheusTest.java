package org.kie.trustyai.service.prometheus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.AdvancedGroupMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;


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
        clearRequests();
        cleanStorage();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        saveDF(dataframe, MODEL_ID);
    }

    abstract void cleanStorage();

    abstract void clearRequests();

    void saveDF(Dataframe dataframe, String modelId) {
        datasource.get().saveDataframe(dataframe, modelId);
        datasource.get().saveMetadata(MockCSVDatasource.createMetadata(dataframe), modelId);
    }

    Pair<String, ScheduleId> createRequest(String endpoint, Object payload) throws InterruptedException {
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
        String metricsList = given()
                .when()
                .basePath("/q/metrics")
                .get().then().statusCode(Response.Status.OK.getStatusCode())
                .extract().body()
                .asString();

        return Pair.of(metricsList, requestId);
    }

    String deleteRequest(String endpoint, ScheduleId requestId) throws InterruptedException {
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

        return metricsListAfterDelete;
    }

    /**
     * Deleting a request should remove it from the Prometheus /q/metrics endpoint
     */
    @Test
    @DisplayName("Deleted single-valued requests are removed from Prometheus metrics endpoint")
    void deleteFairnessRequest() throws InterruptedException {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        String endpoint = "/metrics/group/fairness/spd/request";
        Pair<String, ScheduleId> predeletion = createRequest(endpoint, payload);
        String postDeletion = deleteRequest(endpoint, predeletion.getRight());

        // before deletion, the metrics should exist in the metrics endpoint
        assertThat(predeletion.getLeft(),
                containsString("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\""));

        // after deletion, they should not
        assertThat(postDeletion,
                not(containsString("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\"")));
    }

    @Test
    @DisplayName("Deleted multi-valued requests are removed from Prometheus metrics endpoint")
    void deleteDriftRequest() throws InterruptedException {
        Dataframe taggedDataframe = getTaggedDataframe();
        saveDF(taggedDataframe, MODEL_ID);

        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        String endpoint = "/metrics/drift/meanshift/request";
        Pair<String, ScheduleId> predeletion = createRequest(endpoint, payload);
        String postDeletion = deleteRequest(endpoint, predeletion.getRight());

        for (String column : taggedDataframe.getInputNames()) {
            // before deletion, the metrics should exist in the metrics endpoint
            assertThat(predeletion.getLeft(), containsString("trustyai_meanshift{batch_size=\"5000\",metricName=\"MEANSHIFT\""));
            assertThat(predeletion.getLeft(), containsString("subcategory=\"" + column + "\""));

            // after deletion, they should not
            assertThat(postDeletion, not(containsString("trustyai_meanshift{batch_size=\"5000\",metricName=\"MEANSHIFT\"")));
            assertThat(postDeletion, not(containsString("subcategory=\"" + column + "\"")));
        }
    }

<<<<<<< HEAD
    @ParameterizedTest
    @ValueSource(strings = { "spd", "dir" })
    void advancedBiasLogToPrometheus(String algo) throws InterruptedException {
        final AdvancedGroupMetricRequest payload = RequestPayloadGenerator.advancedCorrect();

        String endpoint = "/metrics/group/fairness/" + algo + "/advanced/request";
        Pair<String, ScheduleId> metricsRequest = createRequest(endpoint, payload);

        String trustyAILines = Arrays.stream(metricsRequest.getLeft()
                .split(System.lineSeparator()))
                .filter(s -> s.contains("trustyai"))
                .collect(Collectors.joining(System.lineSeparator()));

        // the metrics should exist in the metrics endpoint
        assertThat(trustyAILines,
                containsString("trustyai_" + algo + "_advanced{batch_size=\"5000\",favorable_value=\"DataRequestPayload{"));

        deleteRequest(endpoint, metricsRequest.getRight());
    }

    @ParameterizedTest
    @ValueSource(strings = { "spd", "dir" })
    void normalAndAdvancedBiasSimultaneousLogToPrometheus(String algo) throws InterruptedException {
        final AdvancedGroupMetricRequest payload1 = RequestPayloadGenerator.advancedCorrect();
        String endpoint1 = "/metrics/group/fairness/" + algo + "/advanced/request";
        Pair<String, ScheduleId> metricsRequestFirst = createRequest(endpoint1, payload1);

        final GroupMetricRequest payload2 = RequestPayloadGenerator.correct();
        String endpoint2 = "/metrics/group/fairness/" + algo + "/request";
        Pair<String, ScheduleId> metricsAfterBothRequests = createRequest(endpoint2, payload2);

        // only care about metric state after both metrics are registered
        String trustyAILines = Arrays.stream(metricsAfterBothRequests.getLeft()
                .split(System.lineSeparator()))
                .filter(s -> s.contains("trustyai"))
                .collect(Collectors.joining(System.lineSeparator()));

        // make sure we don't have a name conflict
        assertThat(trustyAILines,
                containsString("trustyai_" + algo + "{batch_size=\"5000\",favorable_value=\"1\",metricName="));
        assertThat(trustyAILines,
                containsString("trustyai_" + algo + "_advanced{batch_size=\"5000\",favorable_value=\"DataRequestPayload{"));

        deleteRequest(endpoint1, metricsRequestFirst.getRight());
        deleteRequest(endpoint2, metricsAfterBothRequests.getRight());
=======
    /**
     * Deleting a request should remove it from the Prometheus /q/metrics endpoint
     */
    @Test
    @DisplayName("Single-valued requests  provide name mappings to Prometheus")
    void nameMappedFairnessRequest() throws InterruptedException {
        Map<String, String> inputMapping = new HashMap<>();
        inputMapping.put("gender", "genderMapped");
        Map<String, String> outputMapping = new HashMap<>();
        outputMapping.put("income", "incomeMapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);
        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post("info/names")
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setProtectedAttribute("genderMapped");
        payload.setOutcomeName("incomeMapped");
        Pair<String, String> metricsRequests = createThenDeleteRequest("/metrics/group/fairness/spd/request", payload);

        // Metrics should contain the mapped names
        assertThat(metricsRequests.getLeft(),
                containsString("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"incomeMapped\",privileged=\"1\",protected=\"genderMapped\""));
    }

    @Test
    @DisplayName("Multi-valued requests provide name mappings to Prometheus")
    void nameMappedDriftRequest() throws InterruptedException {
        Dataframe taggedDataframe = getTaggedDataframe();
        saveDF(taggedDataframe, MODEL_ID);

        Map<String, String> inputMapping = new HashMap<>();
        inputMapping.put("age", "ageMapped");
        inputMapping.put("gender", "genderMapped");
        inputMapping.put("race", "raceMapped");
        Map<String, String> outputMapping = new HashMap<>();
        outputMapping.put("income", "incomeMapped");

        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);
        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post("info/names")
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        MeanshiftMetricRequest payload = new MeanshiftMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        Pair<String, String> metricsRequests = createThenDeleteRequest("/metrics/drift/meanshift/request", payload);

        String filteredResponseMeanshift = Arrays.stream(metricsRequests.getLeft().split("\n")).filter(x -> x.contains("trustyai_meanshift")).collect(Collectors.joining());
        for (String column : taggedDataframe.getInputNames()) {
            ;
            // the metrics should contain the mapped column names
            assertThat(filteredResponseMeanshift, containsString("trustyai_meanshift{batch_size=\"5000\",metricName=\"MEANSHIFT\""));
            assertThat(filteredResponseMeanshift, containsString("subcategory=\"" + column + "Mapped\""));
        }
>>>>>>> 5304fbd629f4176ead8b34774915e43e17b9adfd
    }
}
