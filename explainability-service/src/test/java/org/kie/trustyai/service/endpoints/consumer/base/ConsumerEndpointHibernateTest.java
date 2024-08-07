package org.kie.trustyai.service.endpoints.consumer.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
public class ConsumerEndpointHibernateTest extends ConsumerEndpointBaseTest {

    @Inject
    Instance<MockHibernateDatasource> datasource;

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockPrometheusScheduler> scheduler;

    @Override
    void clearStorage() throws JsonProcessingException {
        for (String modelId : datasource.get().getKnownModels()) {
            storage.get().clearData(modelId);
        }
        datasource.get().reset();
        scheduler.get().empty();
    }

    @Override
    @BeforeEach
    @AfterEach
    public void resetDatasource() throws JsonProcessingException {
        clearStorage();
        datasource.get().reset();
    }

    @Override
    public String missingDataMessage(String modelId) {
        return "Error reading dataframe for model=" + modelId + ": Error reading dataframe for model=" + modelId + ": No inference data for that model found in database.";
    }
}
