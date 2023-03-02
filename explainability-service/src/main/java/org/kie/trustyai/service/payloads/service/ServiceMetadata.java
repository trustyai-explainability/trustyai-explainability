package org.kie.trustyai.service.payloads.service;

import org.kie.trustyai.service.data.metadata.Metadata;

public class ServiceMetadata {

    private ServiceMetricsMetadata metrics = new ServiceMetricsMetadata();
    private Metadata data = new Metadata();

    public ServiceMetadata() {

    }

    public ServiceMetricsMetadata getMetrics() {
        return metrics;
    }

    public void setMetrics(ServiceMetricsMetadata metrics) {
        this.metrics = metrics;
    }

    public Metadata getData() {
        return data;
    }

    public void setData(Metadata data) {
        this.data = data;
    }

}
