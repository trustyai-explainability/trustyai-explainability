package org.kie.trustyai.service.endpoints.metrics.drift;

import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;

public abstract class DriftEndpoint extends BaseEndpoint<DriftMetricRequest> {
    protected DriftEndpoint(String name) {
        super(name);
    }

    public abstract MetricThreshold thresholdFunction(Number delta, Number metricValue);

}
