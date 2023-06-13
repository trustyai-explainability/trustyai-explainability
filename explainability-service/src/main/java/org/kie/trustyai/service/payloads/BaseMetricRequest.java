package org.kie.trustyai.service.payloads;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.node.ValueNode;

@JsonPropertyOrder({ "protected", "favorable" })
public class BaseMetricRequest {

    private String protectedAttribute;
    private ValueNode favorableOutcome;
    private String outcomeName;

    private ValueNode privilegedAttribute;
    private ValueNode unprivilegedAttribute;

    private String modelId;

    // this is the unique name of this specific request
    private String requestName;

    // this is the name of the metric that this request calculates, e.g., DIR or SPD
    private String metricName;
    private Double thresholdDelta;

    private Integer batchSize;

    public BaseMetricRequest() {
        // Public default no-argument constructor
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

    public String getProtectedAttribute() {
        return protectedAttribute;
    }

    public void setProtectedAttribute(String protectedAttribute) {
        this.protectedAttribute = protectedAttribute;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public ValueNode getFavorableOutcome() {
        return favorableOutcome;
    }

    // raw getters and setters  ================================================
    public void setFavorableOutcome(ValueNode favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public ValueNode getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(ValueNode privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public ValueNode getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(ValueNode unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BaseMetricRequest that = (BaseMetricRequest) o;
        return protectedAttribute.equals(that.protectedAttribute)
                && favorableOutcome.equals(that.favorableOutcome)
                && outcomeName.equals(that.outcomeName)
                && privilegedAttribute.equals(that.privilegedAttribute)
                && unprivilegedAttribute.equals(that.unprivilegedAttribute)
                && metricName.equals(that.metricName)
                && Objects.equals(thresholdDelta, that.thresholdDelta)
                && Objects.equals(batchSize, that.batchSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protectedAttribute,
                favorableOutcome,
                outcomeName,
                privilegedAttribute,
                unprivilegedAttribute,
                thresholdDelta,
                batchSize,
                metricName);
    }
}
