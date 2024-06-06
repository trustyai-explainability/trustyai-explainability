package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.MockCSVDatasource;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(MeanshiftEndpoint.class)
class MeanshiftEndpointMemoryTest extends MeanshiftEndpointBaseTest {
    @Inject
    Instance<MockMemoryStorage> storage;

    @Override
    void clearData() {
        storage.get().emptyStorage();
    }

    @Override
    void saveDF(Dataframe dataframe) {
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(MockCSVDatasource.createMetadata(dataframe), MODEL_ID);
    }
}
