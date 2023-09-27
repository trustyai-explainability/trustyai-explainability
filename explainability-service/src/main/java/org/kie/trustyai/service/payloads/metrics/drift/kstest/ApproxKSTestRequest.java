package org.kie.trustyai.service.payloads.metrics.drift.kstest;

import java.util.Map;
import org.kie.trustyai.metrics.drift.ks_test.GKSketch;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

/*
 * Request for ApproxKSTest Drift
 */

public class ApproxKSTestRequest extends DriftMetricRequest {
    private double signif = 0.05d; // significance of approxKSTest to accept/reject H0 hypothesis
    private double epsilon = 0.001d; // approximation level in GKSketch
    private Map<String, GKSketch> sketchFitting; // training data fitting 

    public void setSketchFitting(Map<String, GKSketch> sketchFitting) {
        this.sketchFitting = sketchFitting;
    }

    public Map<String, GKSketch> getSketchFitting() {
        return sketchFitting;
    }

    public double getSignif() {
        return signif;
    }

    public void setSignif(double signif) {
        this.signif = signif;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

}
