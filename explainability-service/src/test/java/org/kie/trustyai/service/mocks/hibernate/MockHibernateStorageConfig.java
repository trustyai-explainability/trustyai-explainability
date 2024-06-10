package org.kie.trustyai.service.mocks.hibernate;

import java.util.Optional;

import org.kie.trustyai.service.config.storage.StorageConfig;

public class MockHibernateStorageConfig implements StorageConfig {
    @Override
    public Optional<String> dataFilename() {
        return Optional.empty();
    }

    @Override
    public Optional<String> dataFolder() {
        return Optional.empty();
    }
}
