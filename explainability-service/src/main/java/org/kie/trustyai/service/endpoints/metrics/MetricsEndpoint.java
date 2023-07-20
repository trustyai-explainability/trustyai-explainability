package org.kie.trustyai.service.endpoints.metrics;

import javax.ws.rs.core.Response;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

public interface MetricsEndpoint {

    public String getMetricName();

    public Response listRequests();

    public double calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request);
}
