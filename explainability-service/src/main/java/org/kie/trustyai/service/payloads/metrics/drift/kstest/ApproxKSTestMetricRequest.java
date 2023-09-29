package org.kie.trustyai.service.payloads.metrics.drift.kstest;

import java.util.Map;

import org.kie.trustyai.metrics.drift.kstest.GKSketch;
import org.kie.trustyai.service.payloads.data.statistics.GKSketchesDeserializer;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/*
 * Request for ApproxKSTest Drift
 * 
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type",
        defaultImpl = ApproxKSTestMetricRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApproxKSTestMetricRequest.class, name = "ApproxKSTestMetricRequest")
})
public class ApproxKSTestMetricRequest extends DriftMetricRequest {
    private double epsilon = 0.001d; // approximation level in GKSketch

    @JsonDeserialize(using = GKSketchesDeserializer.class)
    private Map<String, GKSketch> sketchFitting; // training data fitting 

    public ApproxKSTestMetricRequest() {
        super();
        setThresholdDelta(0.05); // significance of approxKSTest (signif) to accept/reject H0 hypothesis
    }

    public void setSketchFitting(Map<String, GKSketch> sketchFitting) {
        if (sketchFitting != null) {
            this.setFitColumns(sketchFitting.keySet());
        }
        this.sketchFitting = sketchFitting;
    }

    public Map<String, GKSketch> getSketchFitting() {
        return sketchFitting;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

}
