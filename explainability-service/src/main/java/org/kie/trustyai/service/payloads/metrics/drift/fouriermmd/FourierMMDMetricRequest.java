package org.kie.trustyai.service.payloads.metrics.drift.fouriermmd;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.data.statistics.FourierMMDValuesDeserializer;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Request for Fourier MMD Drift.
 * Specific parameters for the Fourier MMD are defined in the {@link FourierMMDParameters} class.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = FourierMMDMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FourierMMDMetricRequest.class, name = "FourierMMDMetricRequest")
})
public class FourierMMDMetricRequest extends BaseMetricRequest {

    private Double thresholdDelta;
    private String referenceTag;

    private FourierMMDParameters parameters = new FourierMMDParameters();
    private double gamma = 2.0;

    @JsonDeserialize(using = FourierMMDValuesDeserializer.class)
    private Map<String, Object> fitting;

    public FourierMMDMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public FourierMMDParameters getParameters() {
        return parameters;
    }

    public Map<String, Object> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, Object> fitting) {
        this.fitting = fitting;
    }

    public void setParameters(FourierMMDParameters parameters) {
        this.parameters = parameters;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma() {
        this.gamma = gamma;
    }

    public String getReferenceTag() {
        return referenceTag;
    }

    public void setReferenceTag(String referenceTag) {
        this.referenceTag = referenceTag;
    }

    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }
}
