package org.kie.trustyai.service.endpoints.consumer.cloudevent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.data.storage.Storage;
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
public class CloudEventConsumerMemoryTest extends CloudEventConsumerBaseTest {
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
    Storage getStorage() {
        return storage.get();
    }
}
