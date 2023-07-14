package org.kie.trustyai.service.payloads.metrics;

import io.micrometer.core.instrument.Tags;

import java.util.Map;

public abstract class BaseMetricRequest {
    private String modelId;
    private String requestName;     // this is the unique name of this specific request
    private String metricName;   // this is the name of the metric that this request calculates, e.g., DIR or SPD

    private Integer batchSize;

    public BaseMetricRequest() {}

    public String getModelId() { return modelId;}

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public abstract Map<String, String> retrieveTags();
}
