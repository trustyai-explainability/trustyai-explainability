package org.kie.trustyai.service.payloads.spd;

import org.kie.trustyai.service.payloads.BaseMetricResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;

public class GroupStatisticalParityDifferenceResponse extends BaseMetricResponse {

    private String name = "SPD";
    private MetricThreshold thresholds;

    public GroupStatisticalParityDifferenceResponse(Double value, String specificDefinition, MetricThreshold thresholds) {
        super(value, specificDefinition);
        this.thresholds = thresholds;
    }

    public MetricThreshold getThresholds() {
        return thresholds;
    }

    public void setThresholds(MetricThreshold thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
