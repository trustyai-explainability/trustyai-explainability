package org.kie.trustyai.service.payloads;

import java.util.Date;
import java.util.UUID;

public abstract class BaseMetricResponse {
    public final Date timestamp = new Date();
    protected String type = "metric";
    protected Double value;
    protected String specificDefinition;
    protected UUID id;

    protected BaseMetricResponse(Double value, String specificDefinition) {
        this.value = value;
        this.id = UUID.randomUUID();
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

    public abstract String getName();

    public abstract void setName(String name);

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
