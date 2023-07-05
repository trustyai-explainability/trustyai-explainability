package org.kie.trustyai.service.endpoints.metrics;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface MetricsEndpoint {

    public String getMetricName();

    public Response listRequests();
}
