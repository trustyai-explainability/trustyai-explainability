package org.kie.trustyai.service.endpoints.explainers.local;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.utils.models.TestModels;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.endpoints.explainers.ExplainersEndpointTestProfile;
import org.kie.trustyai.service.endpoints.explainers.GrpcMockServer;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockMemoryStorage;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;
import org.kie.trustyai.service.payloads.explainers.tssaliency.TSSaliencyExplanationRequest;
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
@TestHTTPEndpoint(TSSaliencyEndpoint.class)
class TSSaliencyEndpointTest {

    private static final String MODEL_ID = "example1";
    private static final int N_SAMPLES = 100;
    @Inject
    Instance<MockCSVDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;
    private GrpcMockServer mockServer;

    private void testServiceUrl(String serviceUrl, int expectedStatusCode) throws JsonProcessingException {
        datasource.get().reset();
        final Dataframe _dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);
        final StorageMetadata _metadata = datasource.get().createMetadata(_dataframe);
        final String INPUT_NAME = "custom-input-a";
        final String OUTPUT_NAME = "custom-output-a";
        _metadata.setInputTensorName(INPUT_NAME);
        _metadata.setOutputTensorName(OUTPUT_NAME);
        datasource.get().saveDataframe(_dataframe, MODEL_ID);
        datasource.get().saveMetadata(_metadata, MODEL_ID);
        final Dataframe dataframe = datasource.get().getDataframe(MODEL_ID);
        final List<String> ids = dataframe.getIds().subList(0, 10);
        final TSSaliencyExplanationRequest payload = new TSSaliencyExplanationRequest();
        payload.getConfig().setModelConfig(new ModelConfig(serviceUrl, MODEL_ID, ""));
        payload.setPredictionIds(ids);

        given().contentType(ContentType.JSON).body(payload)
                .when().post()
                .then()
                .statusCode(expectedStatusCode);
    }

    @BeforeEach
    void populateStorage() throws IOException {
        storage.get().emptyStorage();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
        mockServer = new GrpcMockServer(TestModels.getSumSkipModel(1));
        mockServer.start();

    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @Test
    void get() {
        when().get()
                .then()
                .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode())
                .body(is(""));
    }

    @Test
    void testWithValidServiceUrl() throws JsonProcessingException {
        testServiceUrl("http://foo", Response.Status.OK.getStatusCode());
        testServiceUrl("https://bar", Response.Status.OK.getStatusCode());
        testServiceUrl("foo", Response.Status.OK.getStatusCode());
        testServiceUrl("bar:8080", Response.Status.OK.getStatusCode());
    }

    @Test
    void testWithInvalidServiceUrl() throws JsonProcessingException {
        testServiceUrl("http://foo.namespace.svc.cluster.local", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("foo.namespace.svc.cluster.local", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("http://foo/some/path", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("foo/some/path", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        testServiceUrl("", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
