package org.kie.trustyai.service.config.storage;

import java.util.Optional;

public class CustomStorageConfig implements StorageConfig {
    final String storedDataFilename;
    final String storedDataFolder;

    public CustomStorageConfig(String dataFilename, String dataFolder) {
        this.storedDataFilename = dataFilename;
        this.storedDataFolder = dataFolder;
    }

    public Optional<String> dataFilename() {
        return Optional.of(storedDataFilename);
    }

    public Optional<String> dataFolder() {
        return Optional.of(storedDataFolder);
    }

}
