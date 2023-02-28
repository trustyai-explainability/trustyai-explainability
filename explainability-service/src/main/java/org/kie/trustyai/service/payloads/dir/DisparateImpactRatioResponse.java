package org.kie.trustyai.service.payloads.dir;

import org.kie.trustyai.service.payloads.BaseMetricResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;

public class DisparateImpactRatioResponse extends BaseMetricResponse {

    private MetricThreshold thresholds;
    private String name = "DIR";

    public DisparateImpactRatioResponse(Double value, String specificDefinition, MetricThreshold threshold) {
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
