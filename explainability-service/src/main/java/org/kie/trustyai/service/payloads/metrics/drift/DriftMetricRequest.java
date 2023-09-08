package org.kie.trustyai.service.payloads.metrics.drift;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.kie.trustyai.service.payloads.data.statistics.StatisticalSummaryValuesDeserializer;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = DriftMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DriftMetricRequest.class, name = "DriftMetricRequest")
})
public class DriftMetricRequest extends BaseMetricRequest {
    private Double thresholdDelta;
    private String referenceTag;

    @JsonDeserialize(using = StatisticalSummaryValuesDeserializer.class)
    private Map<String,  StatisticalSummaryValues> fitting;

    public DriftMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public Map<String, StatisticalSummaryValues> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, StatisticalSummaryValues> fitting) {
        this.fitting = fitting;
    }

    public String getReferenceTag() {
        return referenceTag;
    }

    public void setReferenceTag(String referenceTag) {
        this.referenceTag = referenceTag;
    }

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }
}
