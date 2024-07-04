package org.kie.trustyai.service.endpoints.explainers.global;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.endpoints.explainers.ExplainersEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.explainers.GlobalExplanationRequest;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

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
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(ExplainersEndpointTestProfile.class)
@TestHTTPEndpoint(AggregatedLimeEndpoint.class)
class AggregatedLimeEndpointTest {

    private static final String MODEL_ID = "example1";
    private static final int N_SAMPLES = 100;
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void populateStorage() {
        storage.get().emptyStorage();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @Test
    void get() {
        when().get()
                .then()
                .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode())
                .body(is(""));
    }

    @Test
    void postWithoutKserve() throws JsonProcessingException {
        datasource.get().reset();
        final GlobalExplanationRequest payload = new GlobalExplanationRequest();
        payload.setModelConfig(new ModelConfig("", MODEL_ID, ""));

        given().contentType(ContentType.JSON).body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
