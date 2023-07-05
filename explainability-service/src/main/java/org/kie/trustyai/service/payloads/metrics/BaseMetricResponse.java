package org.kie.trustyai.service.payloads.metrics;

import java.util.Date;
import java.util.UUID;

public class BaseMetricResponse {
    public final Date timestamp = new Date();
    protected String type = "metric";
    protected Double value;
    protected String specificDefinition;
    protected String name;
    protected UUID id;
    protected MetricThreshold threshold;

    public BaseMetricResponse(Double value, String specificDefinition, MetricThreshold threshold, String name) {
        this.value = value;
        this.id = UUID.randomUUID();
        this.name = name;
        this.threshold = threshold;
        this.specificDefinition = specificDefinition;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getName() { return this.getName(); }

    public void setName(String name) { this.name = name; }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getSpecificDefinition() {
        return specificDefinition;
    }

    public void setSpecificDefinition(String specificDefinition) {
        this.specificDefinition = specificDefinition;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
