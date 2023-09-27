package org.kie.trustyai.service.payloads.metrics.drift.kstest;

import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

/**
 * Request for KSTest Drift.
 * 
 */
public class KSTestRequest extends DriftMetricRequest {

    private double signif = 0.05d; // significance of kstest to accept/reject H0 hypothesis

    public double getSignif() {
        return signif;
    }

    public void setSignif(double signif) {
        this.signif = signif;
    }
}