package org.kie.trustyai.service.endpoints.metrics;

import javax.ws.rs.core.Response;

public interface MetricsEndpoint {

    String getMetricName();

    Response listRequests();

}
