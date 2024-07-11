package org.kie.trustyai.service.endpoints.service.inferenceids;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockPVCStorage;
import org.kie.trustyai.service.profiles.flatfile.PVCTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
public class InferenceIdsServiceMetadataEndpointPVCTest extends InferenceIdsServiceMetadataEndpointBaseTest {

    @Inject
    Instance<MockPVCStorage> storage;

    @Inject
    Instance<MockCSVDatasource> datasource;

    @BeforeEach
    void reset() throws IOException {
        for (String model : datasource.get().getKnownModels()) {
            storage.get().emptyStorage("/tmp/" + model + "-data.csv");
            storage.get().emptyStorage("/tmp/" + model + "-internal_data.csv");
            storage.get().emptyStorage("/tmp/" + model + "-metadata.json");
        }
    }

    @Override
    public void resetDatasource() throws JsonProcessingException {
        datasource.get().reset();
    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) {
        datasource.get().saveDataframe(dataframe, modelId);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), modelId);
    }
}
