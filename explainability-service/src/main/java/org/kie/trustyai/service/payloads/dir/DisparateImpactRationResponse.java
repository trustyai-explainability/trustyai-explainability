package org.kie.trustyai.service.payloads.dir;

import org.kie.trustyai.service.payloads.BaseMetricResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;

public class DisparateImpactRationResponse extends BaseMetricResponse {

    public String name = "DIR";

    public MetricThreshold thresholds;

    public DisparateImpactRationResponse(Double value, MetricThreshold threshold) {
        super(value);
        this.thresholds = threshold;
    }
}
