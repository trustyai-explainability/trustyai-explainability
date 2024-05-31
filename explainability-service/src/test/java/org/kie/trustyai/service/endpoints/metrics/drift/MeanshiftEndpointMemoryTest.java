package org.kie.trustyai.service.endpoints.metrics.drift;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpointTestProfile;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(MeanshiftEndpoint.class)
class MeanshiftEndpointMemoryTest extends MeanshiftEndpointBaseTest {
    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage();
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(N_SAMPLES);

        HashMap<String, List<List<Integer>>> tagging = new HashMap<>();
        tagging.put(TRAINING_TAG, List.of(List.of(0, N_SAMPLES)));
        dataframe.tagDataPoints(tagging);
        dataframe.addPredictions(DataframeGenerators.generateRandomDataframeDrifted(N_SAMPLES).asPredictions());
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }
}
