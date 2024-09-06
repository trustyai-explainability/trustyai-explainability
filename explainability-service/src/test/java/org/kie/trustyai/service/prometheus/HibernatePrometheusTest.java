package org.kie.trustyai.service.prometheus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.GroupStatisticalParityDifferenceEndpoint;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernatePrometheusTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernatePrometheusTestProfile.class)
public class HibernatePrometheusTest extends BasePrometheusTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    protected static final String MODEL_ID = "example1";
    protected static final int N_SAMPLES = 100;

    @AfterEach
    void cleanStorage() {
        // Empty mock storage
        storage.get().clearData(MODEL_ID);
    }
}
