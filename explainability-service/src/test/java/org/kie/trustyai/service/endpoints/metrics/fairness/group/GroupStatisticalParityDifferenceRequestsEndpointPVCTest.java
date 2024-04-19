package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.MockPVCStorage;
import org.kie.trustyai.service.profiles.PVCTestProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceRequestsEndpointPVCTest extends GroupStatisticalParityDifferenceRequestsEndpointBaseTest {

    @Inject
    Instance<MockPVCStorage> storage;

    @BeforeEach
    void populateStorage() {
        // Empty mock storage
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");

        // Clear any requests between tests
        scheduler.get().getAllRequestsFlat().clear();
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

}
