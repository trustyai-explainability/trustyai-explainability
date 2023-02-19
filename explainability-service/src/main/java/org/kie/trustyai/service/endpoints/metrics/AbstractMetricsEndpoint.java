package org.kie.trustyai.service.endpoints.metrics;

import javax.ws.rs.core.Response;

public abstract class AbstractMetricsEndpoint {

    AbstractMetricsEndpoint() {

    }

    public abstract String getMetricName();

    public abstract Response listRequests();

}
