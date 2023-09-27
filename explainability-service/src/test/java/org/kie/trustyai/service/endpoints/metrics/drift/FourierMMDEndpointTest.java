package org.kie.trustyai.service.endpoints.metrics.drift;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDFitting;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDParameters;

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

    final public static String trainDataSetFileName = "train_ts_x.csv";
    final public static String validDataSetFileName = "valid_ts_x.csv";
    final public static String testDataSetFileName = "test_ts_x.csv";

    public Dataframe readCSV(String fileName) {

        BufferedReader br = null;
        List<Prediction> predictions = null;
        try {
            final InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            final InputStreamReader isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            br.readLine(); // skip header line

            predictions = new ArrayList<Prediction>();

            while (true) {

                final String line = br.readLine();
                if (line == null) {
                    break;
                }

                String[] values = line.split(",");
                assert values.length == 13;

                final Feature x1 = new Feature("X1", Type.NUMBER, new Value(Double.parseDouble(values[2])));
                final Feature x2 = new Feature("X2", Type.NUMBER, new Value(Double.parseDouble(values[3])));
                final Feature x3 = new Feature("X3", Type.NUMBER, new Value(Double.parseDouble(values[4])));
                final Feature x4 = new Feature("X4", Type.NUMBER, new Value(Double.parseDouble(values[5])));
                final Feature x5 = new Feature("X5", Type.NUMBER, new Value(Double.parseDouble(values[6])));
                final Feature x6 = new Feature("X6", Type.NUMBER, new Value(Double.parseDouble(values[7])));
                final Feature x7 = new Feature("X7", Type.NUMBER, new Value(Double.parseDouble(values[8])));
                final Feature x8 = new Feature("X8", Type.NUMBER, new Value(Double.parseDouble(values[9])));
                final Feature x9 = new Feature("X9", Type.NUMBER, new Value(Double.parseDouble(values[10])));
                final Feature x10 = new Feature("X10", Type.NUMBER, new Value(Double.parseDouble(values[11])));

                final List<Feature> features = new ArrayList<Feature>(
                        Arrays.asList(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10));

                final PredictionInput predIn = new PredictionInput(features);
                final PredictionOutput predOut = new PredictionOutput(new ArrayList<Output>());
                final Prediction prediction = new SimplePrediction(predIn, predOut);

                predictions.add(prediction);
            }
        } catch (Exception e) {
            throw new IllegalStateException("unexpected Exception", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    //
                }
            }
        }

        return Dataframe.createFrom(predictions);
    }

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage();
        // Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        Dataframe dataframe = readCSV(trainDataSetFileName);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        Dataframe validDF = readCSV(validDataSetFileName);
        dataframe.addPredictions(validDF.asPredictions());
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        scheduler.get().getAllRequests().clear();
    }

    @Test
    void fourierMMDNonPreFit() {
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

        // System.out.println("pvalue" + response.getNamedValues().get("pValue"));
        assertEquals(0, response.getNamedValues().get("pValue"));
    }

    @Test
    void fourierMMDPreFit() {
        Dataframe dfTrain = datasource.get().getDataframe(MODEL_ID).filterRowsByTagEquals(TRAINING_TAG);
        FourierMMDParameters parameters = new FourierMMDParameters();
        FourierMMDFitting fmf = FourierMMD.precompute(dfTrain,
                parameters.getDeltaStat(),
                parameters.getnTest(),
                parameters.getnWindow(),
                parameters.getSig(),
                parameters.getRandomSeed(),
                parameters.getnMode());

        FourierMMDMetricRequest payload = new FourierMMDMetricRequest();
        payload.setReferenceTag(TRAINING_TAG);
        payload.setModelId(MODEL_ID);
        payload.setFitting(fmf.getFitStats());

        BaseMetricResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseMetricResponse.class);

        assertEquals(0, response.getNamedValues().get("pValue"));
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
