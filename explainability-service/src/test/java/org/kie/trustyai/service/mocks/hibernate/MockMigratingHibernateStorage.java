package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;

import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
public class MockMigratingHibernateStorage extends HibernateStorage {
    public MockMigratingHibernateStorage() {
        super(new MockHibernateServiceConfig(), MockHibernateStorageConfigFactory.migratingMock());
    }

}