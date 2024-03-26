package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.io.IOException;

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
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointPVCTest extends DisparateImpactRatioEndpointBaseTest {
    @Inject
    Instance<MockPVCStorage> storage;

    @BeforeEach
    void reset() throws IOException {

        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");

        final Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }
}
