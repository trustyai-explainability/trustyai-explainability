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

    private int batchSize;

    public BaseMetricRequest() {
        // Public default no-argument constructor
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
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

    @Override
    public int hashCode() {
        return Objects.hash(protectedAttribute, favorableOutcome, outcomeName, privilegedAttribute, unprivilegedAttribute);
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BaseMetricRequest that = (BaseMetricRequest) o;
        return batchSize == that.batchSize && Objects.equals(protectedAttribute, that.protectedAttribute) && Objects.equals(favorableOutcome, that.favorableOutcome)
                && Objects.equals(outcomeName, that.outcomeName) && Objects.equals(privilegedAttribute, that.privilegedAttribute) && Objects.equals(unprivilegedAttribute, that.unprivilegedAttribute)
                && Objects.equals(modelId, that.modelId);
    }
}
