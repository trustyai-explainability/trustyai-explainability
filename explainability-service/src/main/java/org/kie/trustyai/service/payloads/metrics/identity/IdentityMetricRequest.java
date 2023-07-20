package org.kie.trustyai.service.payloads.metrics.identity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder({ "protected", "favorable" })
public class IdentityMetricRequest extends BaseMetricRequest {
    private String columnName;
    private double lowerThresh;
    private double upperThresh;

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

    public double getLowerThresh() {
        return lowerThresh;
    }

    public void setLowerThresh(double lowerThresh) {
        this.lowerThresh = lowerThresh;
    }

    public double getUpperThresh() {
        return upperThresh;
    }

    public void setUpperThresh(double upperThresh) {
        this.upperThresh = upperThresh;
    }

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("outcome", this.getColumnName());
        tags.put("feature", this.getColumnName());
        return tags;
    }
}
