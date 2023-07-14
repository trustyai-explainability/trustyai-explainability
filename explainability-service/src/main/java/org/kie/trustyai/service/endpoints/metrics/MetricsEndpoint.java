package org.kie.trustyai.service.endpoints.metrics;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.validators.metrics.ValidBaseMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import javax.ws.rs.core.Response;

public interface MetricsEndpoint {

    public String getMetricName();

    public Response listRequests();
}
