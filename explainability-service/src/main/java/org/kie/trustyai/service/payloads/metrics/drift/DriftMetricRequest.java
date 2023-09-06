package org.kie.trustyai.service.payloads.metrics.drift;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.data.statistics.ColumnSummaryPayload;
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
    private double lowerThreshold;
    private double upperThreshold;
    private Map<String, ColumnSummaryPayload> fitting;

    public DriftMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public double getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(double lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    public double getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(double upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public Map<String, ColumnSummaryPayload> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, ColumnSummaryPayload> fitting) {
        this.fitting = fitting;
    }

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }
}
