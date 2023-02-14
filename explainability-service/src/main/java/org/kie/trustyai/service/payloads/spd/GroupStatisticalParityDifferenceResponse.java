package org.kie.trustyai.service.payloads.spd;

import org.kie.trustyai.service.payloads.BaseMetricResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;

public class GroupStatisticalParityDifferenceResponse extends BaseMetricResponse {

    public String name = "SPD";
    public MetricThreshold thresholds;

    public GroupStatisticalParityDifferenceResponse(Double value, MetricThreshold thresholds) {
        super(value);
        this.thresholds = thresholds;
    }
}
