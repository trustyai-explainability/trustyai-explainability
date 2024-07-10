package org.kie.trustyai.service.endpoints.service.inferenceids;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.payloads.service.InferenceId;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
abstract public class InferenceIdsServiceMetadataEndpointBaseTest {

    public static final String MODEL_ID = "example1";

    @Inject
    Instance<DataSource> datasource;

    @AfterEach
    public abstract void resetDatasource() throws JsonProcessingException;

    public abstract void saveDataframe(Dataframe dataframe, String modelId);

    private static String getEndpoint(String model) {
        return "/info/inference/ids/" + model;
    }

    private static String getEndpointAll(String model) {
        return "/info/inference/ids/" + model + "?type=all";
    }

    private static String getEndpointOrganic(String model) {
        return "/info/inference/ids/" + model + "?type=organic";
    }

    @Test
    @DisplayName("When no data is present in storage")
    void getNoObservationsAtAll() throws JsonProcessingException {
        resetDatasource();
        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
            given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                    .body(is("No metadata found for model=" + MODEL_ID + ". This can happen if TrustyAI has not yet logged any inferences from this model."));
        });
    }

    @Test
    @DisplayName("When there's a wrong type requests")
    void getNoObservationsWrongType() throws JsonProcessingException {
        resetDatasource();
        final int N = 1000;
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N, 10, false);

        saveDataframe(dataframe, MODEL_ID);

        List.of("/info/inference/ids/" + MODEL_ID + "?type=foo", "/info/inference/ids/" + MODEL_ID + "?type=bar").forEach(endpoint -> {
            given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                    .body(is("Invalid type parameter. Valid values must be in ['organic', 'all']."));
        });
    }

    @Test
    @DisplayName("When data is present, but not for the requested model")
    void getNoObservationsModel() throws JsonProcessingException {
        resetDatasource();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000, 10, false);

        saveDataframe(dataframe, MODEL_ID + "_other");

        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
            given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                    .body(is("No metadata found for model=" + MODEL_ID + ". This can happen if TrustyAI has not yet logged any inferences from this model."));
        });
        resetDatasource();
    }

    @Test
    @DisplayName("When data is present for the requested model")
    void getOrganicObservations() throws JsonProcessingException {
        resetDatasource();
        final int N = 1000;
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N, 10, false);

        saveDataframe(dataframe, MODEL_ID);

        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
            final List<InferenceId> inferenceIds = given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body().as(new TypeRef<List<InferenceId>>() {
                    });
            assertEquals(N, inferenceIds.size());
        });

    }

    @Test
    @DisplayName("When only synthetic data is present for the requested model")
    void getSyntheticObservations() throws JsonProcessingException {
        resetDatasource();
        final int N = 1000;
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N, 10, true);

        saveDataframe(dataframe, MODEL_ID);

        given()
                .when().get(getEndpointOrganic(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("No organic inferences found for model=" + MODEL_ID));
        List<InferenceId> inferenceIds = given()
                .when().get(getEndpoint(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<InferenceId>>() {
                });

        assertEquals(N, inferenceIds.size());

        inferenceIds = given()
                .when().get(getEndpointAll(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<InferenceId>>() {
                });

        assertEquals(N, inferenceIds.size());

    }

    @Test
    @DisplayName("When there's synthetic and organic data")
    void getSyntheticAndOrganicObservations() throws JsonProcessingException {
        resetDatasource();
        final int N = 1000;
        final Dataframe syntheticDataframe = DataframeGenerators.generateRandomDataframe(N, 10, true);
        saveDataframe(syntheticDataframe, MODEL_ID);

        final Dataframe organicDataframe = DataframeGenerators.generateRandomDataframe(N * 2, 10, false);
        saveDataframe(organicDataframe, MODEL_ID);

        List<InferenceId> inferenceIds = given()
                .when().get(getEndpoint(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<InferenceId>>() {
                });

        assertEquals(N + N * 2, inferenceIds.size());

        inferenceIds = given()
                .when().get(getEndpointAll(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<InferenceId>>() {
                });

        assertEquals(N + N * 2, inferenceIds.size());

        inferenceIds = given()
                .when().get(getEndpointOrganic(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<InferenceId>>() {
                });

        assertEquals(N * 2, inferenceIds.size());

    }

}
