package org.kie.trustyai.service.endpoints.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.profiles.MemoryTestProfile;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
public class InferenceIdsServiceMetadataEndpointMemoryTest extends InferenceIdsServiceMetadataEndpointBaseTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void reset() {
        storage.get().emptyStorage();
    }

}
