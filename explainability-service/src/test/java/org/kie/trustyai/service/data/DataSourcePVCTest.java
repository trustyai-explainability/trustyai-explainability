package org.kie.trustyai.service.data;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.profiles.PVCTestProfile;
import org.kie.trustyai.service.mocks.MockPVCStorage;

import java.io.IOException;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
public class DataSourcePVCTest extends DataSourceBaseTest {

    @Inject
    Instance<MockPVCStorage> storage;
    @BeforeEach
    void reset() throws IOException {
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");
    }

}
