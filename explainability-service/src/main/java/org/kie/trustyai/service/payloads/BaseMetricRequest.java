package org.kie.trustyai.service.payloads;

import java.util.Objects;

import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "protected", "favorable" })
public class BaseMetricRequest {

    private String protectedAttribute;
    private TypedValue favorableOutcome;
    private String outcomeName;
    private TypedValue privilegedAttribute;
    private TypedValue unprivilegedAttribute;
    private String modelId;
    private String requestName;
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

    public TypedValue getFavorableOutcome() {
        return favorableOutcome;
    }

    public void setFavorableOutcome(TypedValue favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public TypedValue getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(TypedValue privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public TypedValue getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(TypedValue unprivilegedAttribute) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BaseMetricRequest that = (BaseMetricRequest) o;
        return protectedAttribute.equals(that.protectedAttribute) && favorableOutcome.equals(that.favorableOutcome) && outcomeName.equals(that.outcomeName)
                && privilegedAttribute.equals(that.privilegedAttribute) && unprivilegedAttribute.equals(that.unprivilegedAttribute) && thresholdDelta == that.thresholdDelta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protectedAttribute, favorableOutcome, outcomeName, privilegedAttribute, unprivilegedAttribute, thresholdDelta);
    }
}
