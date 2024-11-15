package org.kie.trustyai.service.prometheus;

import org.junit.jupiter.api.AfterEach;
import org.kie.trustyai.service.mocks.flatfile.MockPVCStorage;
import org.kie.trustyai.service.profiles.flatfile.PVCPrometheusTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PVCPrometheusTestProfile.class)
public class PVCPrometheusTest extends BasePrometheusTest {

    @Inject
    Instance<MockPVCStorage> storage;

    @AfterEach
    void cleanStorage() {
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");
    }

}
