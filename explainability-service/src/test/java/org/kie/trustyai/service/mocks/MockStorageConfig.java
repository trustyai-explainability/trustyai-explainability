package org.kie.trustyai.service.mocks;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.kie.trustyai.service.config.storage.StorageConfig;

import io.smallrye.config.SmallRyeConfig;

public class MockStorageConfig implements StorageConfig {
    StorageConfig storageConfig;

    public MockStorageConfig() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        storageConfig = config.getConfigMapping(StorageConfig.class);
    }

    @Override
    public Optional<String> dataFilename() {
        return storageConfig.dataFilename();
    };

    @Override
    public Optional<String> dataFolder() {
        return storageConfig.dataFolder();
    }
}
