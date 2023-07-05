package org.kie.trustyai.service.payloads.metrics;

public abstract class BaseMetricRequest {
    private String modelId;
    private String requestName;     // this is the unique name of this specific request
    private String metricName;   // this is the name of the metric that this request calculates, e.g., DIR or SPD

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

}
