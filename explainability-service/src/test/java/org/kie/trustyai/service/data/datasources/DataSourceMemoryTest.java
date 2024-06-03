package org.kie.trustyai.service.data.datasources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
public class DataSourceMemoryTest extends DataSourceBaseTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @AfterEach
    @BeforeEach
    void reset() {
        storage.get().emptyStorage();
    }

}
