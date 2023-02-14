package org.kie.trustyai.service.payloads.dir;

import org.kie.trustyai.service.payloads.BaseMetricResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;

public class DisparateImpactRatioResponse extends BaseMetricResponse {

    public String name = "DIR";

    public MetricThreshold thresholds;

    public DisparateImpactRatioResponse(Double value, String specificDefinition, MetricThreshold threshold) {
        super(value, specificDefinition);
        this.thresholds = threshold;
    }
}
