package org.kie.trustyai.service.payloads.scheduler;

import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;

import java.util.UUID;

public class ScheduleRequest {
    public UUID id;
    public BaseMetricRequest request;

    public ScheduleRequest() {

    }

    public ScheduleRequest(UUID id, BaseMetricRequest request) {
        this.id = id;
        this.request = request;
    }
}
