package org.kie.trustyai.service.data.datasources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
public class DataSourceHibernateTest extends DataSourceBaseTest {

    @Inject
    Instance<MockHibernateStorage> storage;

    @AfterEach
    @BeforeEach
    void reset() throws JsonProcessingException {
        for (String modelId : datasource.get().getKnownModels()) {
            storage.get().clearData(modelId);
        }
    }
}
