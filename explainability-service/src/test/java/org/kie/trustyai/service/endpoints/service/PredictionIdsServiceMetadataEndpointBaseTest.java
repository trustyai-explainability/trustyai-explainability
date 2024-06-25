package org.kie.trustyai.service.endpoints.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.payloads.service.PredictionId;
import org.kie.trustyai.service.profiles.MemoryTestProfile;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
abstract public class PredictionIdsServiceMetadataEndpointBaseTest {

    public static final String MODEL_ID = "example1";

    @Inject
    Instance<MockDatasource> datasource;

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
        datasource.get().reset();
        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
                    given()
                            .when().get(endpoint)
                            .then()
                            .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                            .body(is("Model ID " + MODEL_ID + " does not exist in TrustyAI metadata."));
                });
    }

    @Test
    @DisplayName("When there's a wrong type requests")
    void getNoObservationsWrongType() throws JsonProcessingException {
        datasource.get().reset();
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
        datasource.get().reset();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 10, false);

        datasource.get().saveDataframe(dataframe, MODEL_ID + "_other");
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID + "_other");

        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
            given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                    .body(is("Model ID " + MODEL_ID + " does not exist in TrustyAI metadata."));
        });
    }

    @Test
    @DisplayName("When data is present for the requested model")
    void getOrganicObservations() throws JsonProcessingException {
        datasource.get().reset();
        final int N = 1000;
        final Dataframe dataframe = datasource.get().generateRandomDataframe(N, 10, false);

        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        List.of(getEndpoint(MODEL_ID), getEndpointAll(MODEL_ID), getEndpointOrganic(MODEL_ID)).forEach(endpoint -> {
            final List<PredictionId> predictionIds = given()
                    .when().get(endpoint)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body().as(new TypeRef<List<PredictionId>>() {
                    });
            assertEquals(N, predictionIds.size());
        });

    }

    @Test
    @DisplayName("When only synthetic data is present for the requested model")
    void getSyntheticObservations() throws JsonProcessingException {
        datasource.get().reset();
        final int N = 1000;
        final Dataframe dataframe = datasource.get().generateRandomDataframe(N, 10, true);

        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        given()
                .when().get(getEndpointOrganic(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(is("Model ID " + MODEL_ID + " does not exist in TrustyAI metadata."));
        List<PredictionId> predictionIds = given()
                .when().get(getEndpoint(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<PredictionId>>() {
                });

        assertEquals(N, predictionIds.size());

        predictionIds = given()
                .when().get(getEndpointAll(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<PredictionId>>() {
                });

        assertEquals(N, predictionIds.size());

    }

    @Test
    @DisplayName("When there's synthetic and organic data")
    void getSyntheticAndOrganicObservations() throws JsonProcessingException {
        datasource.get().reset();
        final int N = 1000;
        final Dataframe syntheticDataframe = datasource.get().generateRandomDataframe(N, 10, true);
        datasource.get().saveDataframe(syntheticDataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(syntheticDataframe), MODEL_ID);
        final Dataframe organicDataframe = datasource.get().generateRandomDataframe(N * 2, 10, false);
        datasource.get().saveDataframe(organicDataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(organicDataframe), MODEL_ID);

        List<PredictionId> predictionIds = given()
                .when().get(getEndpoint(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<PredictionId>>() {
                });

        assertEquals(N + N * 2, predictionIds.size());

        predictionIds = given()
                .when().get(getEndpointAll(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<PredictionId>>() {
                });

        assertEquals(N + N * 2, predictionIds.size());

        predictionIds = given()
                .when().get(getEndpointOrganic(MODEL_ID))
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<PredictionId>>() {
                });

        assertEquals(N * 2, predictionIds.size());


    }

}
