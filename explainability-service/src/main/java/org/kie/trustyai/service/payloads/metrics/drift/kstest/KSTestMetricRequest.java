package org.kie.trustyai.service.payloads.metrics.drift.kstest;

import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = KSTestMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = KSTestMetricRequest.class, name = "KSTestMetricRequest")
})
public class KSTestMetricRequest extends DriftMetricRequest {
    public KSTestMetricRequest() {
        super();
    }
}
