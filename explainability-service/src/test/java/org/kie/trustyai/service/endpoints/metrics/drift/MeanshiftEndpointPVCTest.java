package org.kie.trustyai.service.endpoints.metrics.drift;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.MockCSVDatasource;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.profiles.flatfile.PVCTestProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
@TestHTTPEndpoint(MeanshiftEndpoint.class)
class MeanshiftEndpointPVCTest extends MeanshiftEndpointBaseTest {
    @Inject
    Instance<MockPVCStorage> storage;

    @Override
    void clearData() {
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-internal_data.csv");
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-metadata.json");
    }

    @Override
    void saveDF(Dataframe dataframe) {
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(MockCSVDatasource.createMetadata(dataframe), MODEL_ID);
    }
}
