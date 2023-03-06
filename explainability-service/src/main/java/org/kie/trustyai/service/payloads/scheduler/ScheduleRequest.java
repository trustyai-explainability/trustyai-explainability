package org.kie.trustyai.service.payloads.scheduler;

import java.util.UUID;

import org.kie.trustyai.service.payloads.BaseMetricRequest;

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
