package org.kie.trustyai.service.payloads.metrics.drift.fouriermmd;

import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

/**
 * Request for Fourier MMD Drift.
 * Specific parameters for the Fourier MMD are defined in the {@link FourierMMDParameters} class.
 */
public class FourierMMDMetricRequest extends DriftMetricRequest {

    private FourierMMDParameters parameters = new FourierMMDParameters();

    public FourierMMDParameters getParameters() {
        return parameters;
    }

    public void setParameters(FourierMMDParameters parameters) {
        this.parameters = parameters;
    }
}
