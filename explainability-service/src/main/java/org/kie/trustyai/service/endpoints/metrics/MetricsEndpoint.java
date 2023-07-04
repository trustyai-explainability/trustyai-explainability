package org.kie.trustyai.service.endpoints.metrics;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

import javax.ws.rs.core.Response;

public interface MetricsEndpoint {

    String getMetricName();

    Response listRequests();

    double calculate(Dataframe dataframe, Class<? extends ReconciledBaseMetricRequest> request);

    String getDefinition(double referenceValue, Class<? extends ReconciledBaseMetricRequest> request);
}
