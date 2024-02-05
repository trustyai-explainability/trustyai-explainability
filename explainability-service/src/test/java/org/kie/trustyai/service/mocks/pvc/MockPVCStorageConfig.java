package org.kie.trustyai.service.mocks.pvc;

import org.kie.trustyai.service.config.storage.StorageConfig;

public class MockPVCStorageConfig implements StorageConfig {
    @Override
    public String dataFilename() {
        return "data.csv";
    }

    @Override
    public String dataFolder() {
        return "/tmp";
    }
}
