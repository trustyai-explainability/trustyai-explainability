package org.kie.trustyai.service.config;

import java.util.Optional;
import java.util.OptionalInt;

import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.StorageFormat;

public class CustomServiceConfig implements ServiceConfig {
    final OptionalInt storedBatchSize;
    final StorageFormat storedStorageFormat;
    final Optional<DataFormat> storedDataFormat;
    final String storedMetricsSchedule;

    public CustomServiceConfig(OptionalInt batchSize, StorageFormat storageFormat, DataFormat dataFormat, String metricsSchedule) {
        this.storedBatchSize = batchSize;
        this.storedStorageFormat = storageFormat;
        this.storedDataFormat = Optional.of(dataFormat);
        this.storedMetricsSchedule = metricsSchedule;
    }

    public CustomServiceConfig(OptionalInt batchSize, StorageFormat storageFormat, String metricsSchedule) {
        this.storedBatchSize = batchSize;
        this.storedStorageFormat = storageFormat;
        this.storedDataFormat = Optional.empty();
        this.storedMetricsSchedule = metricsSchedule;
    }

    public OptionalInt batchSize() {
        return storedBatchSize;
    }

    public StorageFormat storageFormat() {
        return storedStorageFormat;
    }

    @Deprecated
    public Optional<DataFormat> dataFormat() {
        return storedDataFormat;
    }

    public String metricsSchedule() {
        return storedMetricsSchedule;
    }
}
