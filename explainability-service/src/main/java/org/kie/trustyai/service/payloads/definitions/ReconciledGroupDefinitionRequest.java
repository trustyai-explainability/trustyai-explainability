package org.kie.trustyai.service.payloads.definitions;

import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

import com.fasterxml.jackson.databind.JsonNode;

public class ReconciledGroupDefinitionRequest extends ReconciledGroupMetricRequest {
    JsonNode metricValue;

    public ReconciledGroupDefinitionRequest(ReconciledGroupMetricRequest reconciledMetricRequest, JsonNode metricValue) {
        super(reconciledMetricRequest.getProtectedAttribute(),
                reconciledMetricRequest.getOutcomeName(),
                reconciledMetricRequest.getModelId(),
                reconciledMetricRequest.getRequestName(),
                reconciledMetricRequest.getMetricName(),
                reconciledMetricRequest.getThresholdDelta(),
                reconciledMetricRequest.getBatchSize(),
                reconciledMetricRequest.getPrivilegedAttribute(),
                reconciledMetricRequest.getFavorableOutcome(),
                reconciledMetricRequest.getUnprivilegedAttribute());
        this.metricValue = metricValue;
    }

    public double getMetricValue() {
        return metricValue.asDouble();
    }

}
