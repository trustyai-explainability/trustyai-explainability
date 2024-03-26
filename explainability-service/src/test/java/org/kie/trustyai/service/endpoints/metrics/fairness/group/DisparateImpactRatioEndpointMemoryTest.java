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
        datasource.get().reset();
        storage.get().emptyStorage();
    }

}
