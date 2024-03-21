package org.kie.trustyai.service.mocks.memory;

import org.kie.trustyai.service.config.storage.MigrationConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.mocks.hibernate.MockMigrationConfigFactory;

public class MockMemoryStorageConfig implements StorageConfig {
    @Override
    public String dataFilename() {
        return "data.csv";
    }

    @Override
    public String dataFolder() {
        return "/tmp";
    }

    @Override
    public MigrationConfig migrationConfig() {
        return MockMigrationConfigFactory.nonMigrating();
    }
}
