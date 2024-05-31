package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.utils.PerColumnStatistics;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@TestHTTPEndpoint(MeanshiftEndpoint.class)
class MeanshiftEndpointHibernateTest extends MeanshiftEndpointBaseTest{
    @Inject
    Instance<MockHibernateStorage> storage;

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().clearData(MODEL_ID);
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(DataframeGenerators.generateRandomDataframeDrifted(N_SAMPLES).asPredictions());
        datasource.get().saveDataframe(dataframe, MODEL_ID);
    }

}
