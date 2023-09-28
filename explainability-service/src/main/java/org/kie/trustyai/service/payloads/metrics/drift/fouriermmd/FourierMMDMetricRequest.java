package org.kie.trustyai.service.payloads.metrics.drift.fouriermmd;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.data.statistics.FourierMMDValuesDeserializer;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

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
public class FourierMMDMetricRequest extends DriftMetricRequest {
    private FourierMMDParameters parameters = new FourierMMDParameters();
    private double gamma = 2.0;

    @JsonDeserialize(using = FourierMMDValuesDeserializer.class)
    private Map<String, Object> fitting;

    public FourierMMDMetricRequest() {
        // Public default no-argument constructor
        super();
    }

    public FourierMMDParameters getParameters() {
        return parameters;
    }


    public Map<String, Object> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, Object> fitting) {
        // fitting object * does not correspond to columns*, so do not set fitColumns
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


    @Override
    public Map<String, String> retrieveTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }
}
