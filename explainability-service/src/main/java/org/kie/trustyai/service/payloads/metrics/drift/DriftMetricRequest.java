package org.kie.trustyai.service.payloads.metrics.drift;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.kstest.ApproxKSTestMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.kstest.KSTestMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.meanshift.MeanshiftMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = DriftMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DriftMetricRequest.class, name = "DriftMetricRequest"),
        @JsonSubTypes.Type(value = MeanshiftMetricRequest.class, name = "MeanshiftMetricRequest"),
        @JsonSubTypes.Type(value = FourierMMDMetricRequest.class, name = "FourierMMDMetricRequest"),
        @JsonSubTypes.Type(value = ApproxKSTestMetricRequest.class, name = "ApproxKSTestMetricRequest"),
        @JsonSubTypes.Type(value = KSTestMetricRequest.class, name = "KSTestMetricRequest"),
})
@JsonTypeName("DriftMetricRequest")
public abstract class DriftMetricRequest extends BaseMetricRequest {
    private Double thresholdDelta;
    private String referenceTag;
    private Set<String> fitColumns;

    protected DriftMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public String getReferenceTag() {
        return referenceTag;
    }

    public void setReferenceTag(String referenceTag) {
        this.referenceTag = referenceTag;
    }

    public Set<String> getFitColumns() {
        return fitColumns;
    }

    public void setFitColumns(Set<String> fitColumns) {
        this.fitColumns = fitColumns;
    }

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }
}
