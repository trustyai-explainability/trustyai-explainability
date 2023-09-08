package org.kie.trustyai.service.payloads.metrics;

import com.google.api.Metric;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseMetricResponse {
    public final Date timestamp = new Date();
    protected String type = "metric";
    protected Double value;
    protected Map<String, Double> namedValues;
    protected String specificDefinition;
    protected String name;
    protected UUID id;
    protected MetricThreshold threshold;

    public BaseMetricResponse(MetricValueCarrier mvc, String specificDefinition, MetricThreshold threshold, String name) {
        if (mvc.isSingle()) {
            this.value = mvc.getValue();
        } else {
            this.namedValues = mvc.getNamedValues();
        }
        this.id = UUID.randomUUID();
        this.name = name;
        this.threshold = threshold;
        this.specificDefinition = specificDefinition;
    }

    protected BaseMetricResponse() {
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Map<String, Double> getNamedValues() {
        return namedValues;
    }

    public void setNamedValues(Map<String, Double> namedValues) {
        this.namedValues = namedValues;
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

    public MetricThreshold getThresholds() {
        return threshold;
    }

    public void setThresholds(MetricThreshold threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return "BaseMetricResponse{" +
                "timestamp=" + timestamp +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", namedValues=" + namedValues +
                ", specificDefinition='" + specificDefinition + '\'' +
                ", name='" + name + '\'' +
                ", id=" + id +
                ", threshold=" + threshold +
                '}';
    }
}
