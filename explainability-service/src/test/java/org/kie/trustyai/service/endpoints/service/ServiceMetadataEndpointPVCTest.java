package org.kie.trustyai.service.endpoints.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
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
class ServiceMetadataEndpointPVCTest extends ServiceMetadataEndpointBaseTest {
    @Inject
    Instance<MockCSVDatasource> datasource;

    @Inject
    Instance<MockPVCStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @BeforeEach
    @AfterEach
    void clearStorage() throws JsonProcessingException {
        for (String modelId : datasource.get().getKnownModels()) {
            storage.get().emptyStorage("/tmp/" + modelId + "-data.csv");
            storage.get().emptyStorage("/tmp/" + modelId + "-internal_data.csv");
            storage.get().emptyStorage("/tmp/" + modelId + "-metadata.json");
        }
        datasource.get().reset();
        scheduler.get().empty();
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
