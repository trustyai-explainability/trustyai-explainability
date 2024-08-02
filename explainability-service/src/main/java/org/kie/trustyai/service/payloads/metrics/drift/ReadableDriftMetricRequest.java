package org.kie.trustyai.service.payloads.metrics.drift;

import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Container for the metric listing endpoint to list drift requests in a human-readable way
 * This is needed to prevent spamming the response with the fitted distributions of the drift metric
 */
@JsonPropertyOrder(alphabetic = true)
public class ReadableDriftMetricRequest extends BaseMetricRequest {
    private final Double thresholdDelta;
    private final String referenceTag;
    private final Set<String> fitColumns;

    @JsonProperty("@type")
    private final String type;

    protected ReadableDriftMetricRequest(DriftMetricRequest request) {
        super();
        this.setMetricName(request.getMetricName());
        this.setRequestName(request.getRequestName());
        this.setModelId(request.getModelId());
        this.setBatchSize(request.getBatchSize());

        this.fitColumns = request.getFitColumns();
        this.thresholdDelta = request.getThresholdDelta();
        this.referenceTag = request.getReferenceTag();
        this.type = request.getClass().getSimpleName();
    }

    @Override
    public Map<String, String> retrieveTags() {
        return Map.of();
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public String getReferenceTag() {
        return referenceTag;
    }

    public Set<String> getFitColumns() {
        return fitColumns;
    }

    public String getType() {
        return type;
    }
}
