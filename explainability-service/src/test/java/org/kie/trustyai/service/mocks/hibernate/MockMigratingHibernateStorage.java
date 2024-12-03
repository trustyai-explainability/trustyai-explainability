package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;
import org.kie.trustyai.service.mocks.MockServiceConfig;
import org.kie.trustyai.service.mocks.MockStorageConfig;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@Priority(1)
@ApplicationScoped
public class MockMigratingHibernateStorage extends HibernateStorage {
    public MockMigratingHibernateStorage() {
        super(new MockServiceConfig(), new MockStorageConfig());
    }

}
