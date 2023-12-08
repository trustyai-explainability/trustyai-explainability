package org.kie.trustyai.service.payloads.metrics.fairness.group;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonPropertyOrder({ "protected", "favorable" })
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = AdvancedGroupMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AdvancedGroupMetricRequest.class, name = "AdvancedGroupMetricRequest")
})
public class AdvancedGroupMetricRequest extends BaseMetricRequest {
    // For any request-provider value that needs to be validated against feature/output types
    public DataRequestPayload privilegedAttribute;
    public DataRequestPayload unprivilegedAttribute;
    public DataRequestPayload favorableOutcome;

    private Double thresholdDelta;

    public AdvancedGroupMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public DataRequestPayload getFavorableOutcome() {
        return favorableOutcome;
    }

    public void setFavorableOutcome(DataRequestPayload favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public DataRequestPayload getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(DataRequestPayload privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public DataRequestPayload getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(DataRequestPayload unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    // Tag Retrieval
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("favorable_value", this.getFavorableOutcome().toString());
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
        AdvancedGroupMetricRequest that = (AdvancedGroupMetricRequest) o;
        return favorableOutcome.equals(that.favorableOutcome)
                && privilegedAttribute.equals(that.privilegedAttribute)
                && unprivilegedAttribute.equals(that.unprivilegedAttribute)
                && this.getMetricName().equals(that.getMetricName())
                && Objects.equals(thresholdDelta, that.thresholdDelta)
                && Objects.equals(getBatchSize(), that.getBatchSize());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                favorableOutcome,
                privilegedAttribute,
                unprivilegedAttribute,
                thresholdDelta,
                getBatchSize(),
                this.getMetricName());
    }
}
