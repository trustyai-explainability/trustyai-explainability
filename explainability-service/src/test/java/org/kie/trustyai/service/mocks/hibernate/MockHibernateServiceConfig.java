package org.kie.trustyai.service.mocks.hibernate;

import java.util.OptionalInt;

import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.StorageFormat;

public class MockHibernateServiceConfig implements ServiceConfig {

    @Override
    public OptionalInt batchSize() {
        return OptionalInt.of(5000);
    }

    @Override
    public StorageFormat storageFormat() {
        return StorageFormat.HIBERNATE;
    }

    @Override
    public DataFormat dataFormat() {
        return DataFormat.HIBERNATE;
    }

    @Override
    public String metricsSchedule() {
        return "5s";
    }
}
