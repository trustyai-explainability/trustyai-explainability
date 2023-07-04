package org.kie.trustyai.service.payloads.definitions;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;

import com.fasterxml.jackson.databind.JsonNode;

public class GroupDefinitionRequest extends GroupMetricRequest {
    public JsonNode metricValue;

    public GroupDefinitionRequest() {
        super();
    }

    public double getMetricValue() {
        return metricValue.asDouble();
    }

}
