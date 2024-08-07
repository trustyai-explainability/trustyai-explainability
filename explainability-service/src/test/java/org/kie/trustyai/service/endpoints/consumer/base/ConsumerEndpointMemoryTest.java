package org.kie.trustyai.service.endpoints.consumer.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class ConsumerEndpointMemoryTest extends ConsumerEndpointBaseTest {
    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockCSVDatasource> datasource;

    @BeforeEach
    void reset() {
        storage.get().emptyStorage();
    }

    @Override
    void clearStorage() {
        reset();
    }

    @Override
    @BeforeEach
    @AfterEach
    public void resetDatasource() throws JsonProcessingException {
        datasource.get().reset();
        reset();
    }

    @Override
    public String missingDataMessage(String modelId) {
        return "Error reading dataframe for model=" + modelId + ": Data file '" + modelId + "-data.csv' not found";
    }
}
