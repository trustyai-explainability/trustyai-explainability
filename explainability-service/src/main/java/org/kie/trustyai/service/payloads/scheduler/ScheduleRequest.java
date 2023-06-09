package org.kie.trustyai.service.payloads.scheduler;

import java.util.UUID;

import org.kie.trustyai.service.payloads.ReconciledMetricRequest;

public class ScheduleRequest {
    public UUID id;
    public ReconciledMetricRequest request;

    public ScheduleRequest() {

    }

    public ScheduleRequest(UUID id, ReconciledMetricRequest request) {
        this.id = id;
        this.request = request;
    }
}
