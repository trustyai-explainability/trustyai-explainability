package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.config.storage.MigrationConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;

public class MockHibernateStorageConfigFactory {

    private static class MockHibernateStorage implements StorageConfig {
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
        public MigrationConfig migrationConfig() {
            return MockMigrationConfigFactory.nonMigrating();
        }
    }

    private static class MockMigratingHibernateStorage extends MockHibernateStorage {
        @Override
        public MigrationConfig migrationConfig() {
            return MockMigrationConfigFactory.migrating();
        }
    }

    public static StorageConfig mock() {
        return new MockHibernateStorage();
    }

    public static StorageConfig migratingMock() {
        return new MockMigratingHibernateStorage();
    }

}
