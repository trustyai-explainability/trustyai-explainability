package org.kie.trustyai.service.payloads.metrics.drift.meanshift;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.kie.trustyai.service.payloads.data.statistics.StatisticalSummaryValuesDeserializer;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = MeanshiftMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MeanshiftMetricRequest.class, name = "MeanshiftMetricRequest")
})
public class MeanshiftMetricRequest extends DriftMetricRequest {
    @JsonDeserialize(using = StatisticalSummaryValuesDeserializer.class)
    private Map<String, StatisticalSummaryValues> fitting;

    public Map<String, StatisticalSummaryValues> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, StatisticalSummaryValues> fitting) {
        if (fitting != null) {
            this.setFitColumns(fitting.keySet());
        }
        this.fitting = fitting;
    }
}
