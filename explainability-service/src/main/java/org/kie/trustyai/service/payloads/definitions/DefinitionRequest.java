package org.kie.trustyai.service.payloads.definitions;

import org.kie.trustyai.service.payloads.BaseMetricRequest;

import com.fasterxml.jackson.databind.JsonNode;

public class DefinitionRequest extends BaseMetricRequest {
    public JsonNode metricValue;

    public DefinitionRequest() {
        super();
    }

    public double getMetricValue() {
        return metricValue.asDouble();
    }

}
