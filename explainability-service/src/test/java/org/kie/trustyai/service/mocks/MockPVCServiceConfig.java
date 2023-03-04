package org.kie.trustyai.service.mocks;

import java.util.Optional;
import java.util.OptionalInt;

import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.StorageFormat;

public class MockPVCServiceConfig implements ServiceConfig {
    @Override
    public OptionalInt batchSize() {
        return OptionalInt.of(5000);
    }

    @Override
    public Optional<String> kserveTarget() {
        return Optional.empty();
    }

    @Override
    public StorageFormat storageFormat() {
        return StorageFormat.PVC;
    }

    @Override
    public DataFormat dataFormat() {
        return DataFormat.CSV;
    }

    @Override
    public String metricsSchedule() {
        return "5s";
    }
}
