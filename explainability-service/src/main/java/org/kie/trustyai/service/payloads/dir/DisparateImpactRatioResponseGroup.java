package org.kie.trustyai.service.payloads.dir;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;

public class DisparateImpactRatioResponseGroup extends GroupMetricResponse {

    private MetricThreshold thresholds;
    private String name = "DIR";

    public DisparateImpactRatioResponseGroup(Double value, String specificDefinition, MetricThreshold threshold) {
        super(value, specificDefinition);
        this.thresholds = threshold;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public MetricThreshold getThresholds() {
        return thresholds;
    }

    public void setThresholds(MetricThreshold thresholds) {
        this.thresholds = thresholds;
    }
}
