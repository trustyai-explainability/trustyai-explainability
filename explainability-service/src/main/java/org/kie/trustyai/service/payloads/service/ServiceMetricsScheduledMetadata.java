package org.kie.trustyai.service.payloads.service;

import java.util.HashMap;
import java.util.Map;

public class ServiceMetricsScheduledMetadata {
    private Map<String, Integer> metricCounts = new HashMap<>();

    public ServiceMetricsScheduledMetadata() {
    }

    public Map<String, Integer> getMetricCounts() {
        return metricCounts;
    }

    public Integer getCount(String metricName) {
        return metricCounts.getOrDefault(metricName, 0);
    }

    public void setCount(String metricName, Integer value) {
        metricCounts.put(metricName, value);
    }
}
