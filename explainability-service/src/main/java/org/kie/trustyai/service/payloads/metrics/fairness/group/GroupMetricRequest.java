package org.kie.trustyai.service.payloads.metrics.fairness.group;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.values.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.ReconcilableOutput;
import org.kie.trustyai.service.payloads.values.ReconcilerMatcher;

@JsonPropertyOrder({ "protected", "favorable" })
public class GroupMetricRequest extends BaseMetricRequest {

    // fields to be reconciled against dataset metadata
    private String protectedAttribute;
    private String outcomeName;

    @ReconcilerMatcher(nameProvider="getProtectedAttribute")
    public ReconcilableFeature privilegedAttribute;

    @ReconcilerMatcher(nameProvider="getProtectedAttribute")
    public ReconcilableFeature unprivilegedAttribute;

    @ReconcilerMatcher(nameProvider="getOutcomeName")
    public ReconcilableOutput favorableOutcome;

    private Double thresholdDelta;

    public GroupMetricRequest() {
        // Public default no-argument constructor
        super();
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

    public ReconcilableOutput getFavorableOutcome() {
        return favorableOutcome;
    }

    // raw getters and setters  ================================================
    public void setFavorableOutcome(ReconcilableOutput favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public ReconcilableFeature getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(ReconcilableFeature privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public ReconcilableFeature getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(ReconcilableFeature unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public Map<String, String> retrieveTags(){
        Map<String, String> tags = new HashMap<>();
        tags.put("outcome", this.getOutcomeName());
        tags.put("favorable_value", this.getFavorableOutcome().toString());
        tags.put("protected", this.getProtectedAttribute());
        tags.put("privileged", this.getPrivilegedAttribute().toString());
        tags.put("unprivileged", this.getUnprivilegedAttribute().toString());
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GroupMetricRequest that = (GroupMetricRequest) o;
        return protectedAttribute.equals(that.protectedAttribute)
                && favorableOutcome.equals(that.favorableOutcome)
                && outcomeName.equals(that.outcomeName)
                && privilegedAttribute.equals(that.privilegedAttribute)
                && unprivilegedAttribute.equals(that.unprivilegedAttribute)
                && this.getMetricName().equals(that.getMetricName())
                && Objects.equals(thresholdDelta, that.thresholdDelta)
                && Objects.equals(getBatchSize(), that.getBatchSize());
    }

    @Override
    public int hashCode() {
        return Objects.hash(protectedAttribute,
                favorableOutcome,
                outcomeName,
                privilegedAttribute,
                unprivilegedAttribute,
                thresholdDelta,
                getBatchSize(),
                this.getMetricName());
    }
}
