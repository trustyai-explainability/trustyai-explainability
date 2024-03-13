package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.config.storage.MigrationConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;

public class MockHibernateStorageConfig implements StorageConfig {

    // not needed
    @Override
    public String dataFilename() {
        return "data.csv";
    }

    // not needed
    @Override
    public String dataFolder() {
        return "/tmp";
    }

    @Override
    public MigrationConfig migrationConfig(){
        return new MockMigrationConfig();
    }
}
