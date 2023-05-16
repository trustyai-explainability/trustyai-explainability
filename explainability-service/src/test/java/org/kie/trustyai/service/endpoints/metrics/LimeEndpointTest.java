package org.kie.trustyai.service.endpoints.metrics;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.endpoints.explainers.LimeEndpoint;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(ExplainersEndpointTestProfile.class)
@TestHTTPEndpoint(LimeEndpoint.class)
class LimeEndpointTest {

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

    //    @Test
    //    void postCorrect() throws JsonProcessingException {
    //        datasource.get().reset();
    //        Dataframe dataframe = datasource.get().getDataframe(MODEL_ID);
    //        List<PredictionInput> predictionInputs = dataframe.asPredictionInputs();
    //        String id = String.valueOf(predictionInputs.get(0).hashCode());
    //        final BaseExplanationRequest payload = new BaseExplanationRequest();
    //        payload.setModelId(MODEL_ID);
    //        payload.setPredictionId(id);
    //
    //        final SaliencyExplanationResponse response = given()
    //                .contentType(ContentType.JSON)
    //                .body(payload)
    //                .when().post()
    //                .then()
    //                .statusCode(Response.Status.OK.getStatusCode())
    //                .extract()
    //                .body().as(SaliencyExplanationResponse.class);
    //
    //        assertNotNull(response.getSaliencies());
    //    }

}
