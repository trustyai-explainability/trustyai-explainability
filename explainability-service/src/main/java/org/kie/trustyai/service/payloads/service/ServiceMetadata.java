package org.kie.trustyai.service.payloads.service;

import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.service.readable.ReadableStorageMetadata;

public class ServiceMetadata {

    private ServiceMetricsMetadata metrics = new ServiceMetricsMetadata();
    private ReadableStorageMetadata data;

    public ServiceMetadata() {
        // empty constructor
    }

    public ServiceMetricsMetadata getMetrics() {
        return metrics;
    }

    public void setMetrics(ServiceMetricsMetadata metrics) {
        this.metrics = metrics;
    }

    public ReadableStorageMetadata getData() {
        return data;
    }

    public void setData(ReadableStorageMetadata data) {
        this.data = data;
    }

}
