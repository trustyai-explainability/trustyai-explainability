package org.kie.trustyai.service.mocks;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.ConfigProvider;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.StorageFormat;

import io.smallrye.config.SmallRyeConfig;

public class MockServiceConfig implements ServiceConfig {
    ServiceConfig serviceConfig;

    public MockServiceConfig() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        serviceConfig = config.getConfigMapping(ServiceConfig.class);
    }

    @Override
    public OptionalInt batchSize() {
        return serviceConfig.batchSize();
    }

    @Override
    public StorageFormat storageFormat() {
        return serviceConfig.storageFormat();
    }

    @Override
    public DataFormat dataFormat() {
        return serviceConfig.dataFormat();
    }

    @Override
    public String metricsSchedule() {
        return serviceConfig.metricsSchedule();
    }
}
