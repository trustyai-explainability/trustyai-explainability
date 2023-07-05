package org.kie.trustyai.service.payloads.metrics;

import java.util.Map;

public class ReconciledBaseMetricRequest {
    private String modelId;

    // this is the unique name of this specific request
    private String requestName;

    // this is the name of the metric that this request calculates, e.g., DIR or SPD
    private String metricName;

    // the tags to place into Prometheus
    private Map<String, String> tags;

    public ReconciledBaseMetricRequest(String modelId, String requestName, String metricName){
        this.metricName = metricName;
        this.modelId = modelId;
        this.requestName = requestName;
    }

    public String getModelId() {
        return modelId;
    }

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

    public Map<String, String> getTags(){ return this.tags; }

    public void setTags(Map<String, String> tags){
        this.tags = tags;
    }
}
