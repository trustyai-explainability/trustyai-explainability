package org.kie.trustyai.service.payloads.requests.explainers.tslime;

import org.kie.trustyai.service.payloads.requests.explainers.TimeSeriesRequest;

/**
 * Request for a time-series TSSaliency explanation.
 * Specific parameters for the TSSaliency explainer are defined in the {@link TSLimeParameters} class.
 */
public class TSLimeRequest extends TimeSeriesRequest {

    private TSLimeParameters parameters = new TSLimeParameters();

    public TSLimeParameters getParameters() {
        return parameters;
    }

    public void setParameters(TSLimeParameters parameters) {
        this.parameters = parameters;
    }
}
