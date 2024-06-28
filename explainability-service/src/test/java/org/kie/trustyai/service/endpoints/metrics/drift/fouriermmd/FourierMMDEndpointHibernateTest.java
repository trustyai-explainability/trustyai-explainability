package org.kie.trustyai.service.endpoints.metrics.drift.fouriermmd;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.endpoints.metrics.drift.FourierMMDEndpoint;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@TestHTTPEndpoint(FourierMMDEndpoint.class)
class FourierMMDEndpointHibernateTest extends FourierMMDEndpointBaseTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    @Override
    void clearData() {
        storage.get().clearData(MODEL_ID);
    }

    @Override
    void saveDF(Dataframe dataframe) {
        datasource.get().saveDataframe(dataframe, MODEL_ID);
    }
}
