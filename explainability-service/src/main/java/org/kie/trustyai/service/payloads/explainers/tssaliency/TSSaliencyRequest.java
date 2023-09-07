package org.kie.trustyai.service.payloads.explainers.tssaliency;

import org.kie.trustyai.service.payloads.explainers.TimeSeriesRequest;

/**
 * Request for a time-series TSSaliency explanation.
 * Specific parameters for the TSSaliency explainer are defined in the {@link TSSaliencyParameters} class.
 */
public class TSSaliencyRequest extends TimeSeriesRequest {

    private TSSaliencyParameters parameters = new TSSaliencyParameters();

    public TSSaliencyParameters getParameters() {
        return parameters;
    }

    public void setParameters(TSSaliencyParameters parameters) {
        this.parameters = parameters;
    }
}
