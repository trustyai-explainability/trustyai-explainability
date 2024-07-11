package org.kie.trustyai.service.endpoints.service.inferenceids;

import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockMemoryStorage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
public class InferenceIdsServiceMetadataEndpointMemoryTest extends InferenceIdsServiceMetadataEndpointBaseTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockCSVDatasource> datasource;

    @BeforeEach
    void reset() {
        storage.get().emptyStorage();
    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) {
        datasource.get().saveDataframe(dataframe, modelId);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), modelId);
    }

    @Override
    public void resetDatasource() throws JsonProcessingException {
        datasource.get().reset();
        reset();
    }
}
