package org.kie.trustyai.service.endpoints.explainers.local;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.endpoints.explainers.ExplainersEndpointTestProfile;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockMemoryStorage;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;
import org.kie.trustyai.service.payloads.explainers.shap.SHAPExplanationRequest;
import org.kie.trustyai.service.utils.DataframeGenerators;

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
@TestHTTPEndpoint(SHAPEndpoint.class)
class ShapEndpointTest {

    private static final String MODEL_ID = "example1";
    private static final int N_SAMPLES = 100;
    @Inject
    Instance<MockCSVDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    private void testServiceUrl(String serviceUrl, int expectedStatusCode) throws JsonProcessingException {
        datasource.get().reset();
        Dataframe dataframe = datasource.get().getDataframe(MODEL_ID);
        List<PredictionInput> predictionInputs = dataframe.asPredictionInputs();
        String id = String.valueOf(predictionInputs.get(0).hashCode());
        final SHAPExplanationRequest payload = new SHAPExplanationRequest();
        payload.getConfig().setModelConfig(new ModelConfig(serviceUrl, MODEL_ID, ""));
        payload.setPredictionId(id);

        given().contentType(ContentType.JSON).body(payload)
                .when().post()
                .then()
                .statusCode(expectedStatusCode);
    }

    @BeforeEach
    void populateStorage() {
        storage.get().emptyStorage();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);
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
        Dataframe dataframe = datasource.get().getDataframe(MODEL_ID);
        List<PredictionInput> predictionInputs = dataframe.asPredictionInputs();
        String id = String.valueOf(predictionInputs.get(0).hashCode());
        final SHAPExplanationRequest payload = new SHAPExplanationRequest();
        payload.getConfig().setModelConfig(new ModelConfig("", MODEL_ID, ""));
        payload.setPredictionId(id);

        given().contentType(ContentType.JSON).body(payload)
                .when().post()
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void testWithValidServiceUrl() throws JsonProcessingException {
        testServiceUrl("http://foo", Response.Status.NOT_FOUND.getStatusCode());
        testServiceUrl("https://bar", Response.Status.NOT_FOUND.getStatusCode());
        testServiceUrl("foo", Response.Status.NOT_FOUND.getStatusCode());
        testServiceUrl("bar:8080", Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void testWithInvalidServiceUrl() throws JsonProcessingException {
        testServiceUrl("http://foo.namespace.svc.cluster.local", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("foo.namespace.svc.cluster.local", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("http://foo/some/path", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("foo/some/path", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
