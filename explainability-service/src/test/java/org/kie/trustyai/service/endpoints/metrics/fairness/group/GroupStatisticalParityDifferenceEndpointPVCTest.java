package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.mocks.MockPVCStorage;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.profiles.MemoryTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.kie.trustyai.service.profiles.PVCTestProfile;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceEndpointPVCTest extends GroupStatisticalParityDifferenceEndpointBaseTest {

    @Inject
    Instance<MockPVCStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @BeforeEach
    void populateStorage() throws JsonProcessingException {
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");

        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        // prevent a failing test from failing other tests erroneously
        scheduler.get().getAllRequests().clear();
    }
    
}
