package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceEndpointHibernateTest extends GroupStatisticalParityDifferenceEndpointBaseTest {

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    void populate() {
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
    }

    @BeforeEach
    void reset() {
        storage.get().clearData(MODEL_ID);
    }

    @AfterEach
    void clearRequests() {
        // prevent a failing test from failing other tests erroneously
        scheduler.get().getAllRequests().clear();
    }

}
