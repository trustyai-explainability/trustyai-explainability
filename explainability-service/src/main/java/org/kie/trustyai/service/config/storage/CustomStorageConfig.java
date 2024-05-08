package org.kie.trustyai.service.config.storage;

public class CustomStorageConfig implements StorageConfig {
    final String storedDataFilename;
    final String storedDataFolder;
    final MigrationConfig storedMigrationConfig;

    public CustomStorageConfig(String dataFilename, String dataFolder, MigrationConfig migrationConfig) {
        this.storedDataFilename = dataFilename;
        this.storedDataFolder = dataFolder;
        this.storedMigrationConfig = migrationConfig;
    }

    public String dataFilename() {
        return storedDataFilename;
    }

    public String dataFolder() {
        return storedDataFolder;
    }

    public MigrationConfig migrationConfig() {
        return storedMigrationConfig;
    }
}
