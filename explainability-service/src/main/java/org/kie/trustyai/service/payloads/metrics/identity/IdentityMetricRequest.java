package org.kie.trustyai.service.payloads.metrics.identity;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonPropertyOrder({ "protected", "favorable" })
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = IdentityMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = IdentityMetricRequest.class, name = "IdentityMetricRequest")
})
public class IdentityMetricRequest extends BaseMetricRequest {
    private String columnName;
    private double lowerThreshold;
    private double upperThreshold;

    public IdentityMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
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

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("column", this.getColumnName());
        return tags;
    }
}
