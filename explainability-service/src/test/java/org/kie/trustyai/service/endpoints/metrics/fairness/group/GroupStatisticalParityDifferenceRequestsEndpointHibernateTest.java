package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.profiles.flatfile.PVCTestProfile;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceRequestsEndpointHibernateTest extends GroupStatisticalParityDifferenceRequestsEndpointBaseTest {

    @Inject
    Instance<MockHibernateStorage> storage;

    @BeforeEach
    void populateStorage() {
        // Empty mock storage
        storage.get().clearData(MODEL_ID);

        // Clear any requests between tests
        scheduler.get().getAllRequestsFlat().clear();
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);
    }

}
