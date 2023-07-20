package org.kie.trustyai.service.payloads.metrics;

import java.util.Map;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.metrics.identity.IdentityMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GroupMetricRequest.class, name = "GroupMetricRequest"),
        @JsonSubTypes.Type(value = IdentityMetricRequest.class, name = "IdentityMetricRequest")
})
public abstract class BaseMetricRequest {
    private String modelId;
    private String requestName; // this is the unique name of this specific request
    private String metricName; // this is the name of the metric that this request calculates, e.g., DIR or SPD

    private Integer batchSize;

    protected BaseMetricRequest() {
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

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public abstract Map<String, String> retrieveTags();
}
