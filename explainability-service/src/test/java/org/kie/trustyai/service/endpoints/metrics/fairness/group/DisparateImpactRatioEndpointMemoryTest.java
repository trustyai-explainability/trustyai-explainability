package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.profiles.MemoryTestProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointMemoryTest extends DisparateImpactRatioEndpointBaseTest {
    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void reset() throws IOException {

        storage.get().emptyStorage();

        final Dataframe dataframe = datasource.get().generateRandomDataframe(N_SAMPLES);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

}
