package org.kie.trustyai.service.payloads.service;

import org.kie.trustyai.service.data.metadata.StorageMetadata;

public class ServiceMetadata {

    private ServiceMetricsMetadata metrics = new ServiceMetricsMetadata();
    private StorageMetadata data = new StorageMetadata();

    public ServiceMetadata() {
        // empty constructor
    }

    public ServiceMetricsMetadata getMetrics() {
        return metrics;
    }

    public void setMetrics(ServiceMetricsMetadata metrics) {
        this.metrics = metrics;
    }

    public StorageMetadata getData() {
        return data;
    }

    public void setData(StorageMetadata data) {
        this.data = data;
    }

}
