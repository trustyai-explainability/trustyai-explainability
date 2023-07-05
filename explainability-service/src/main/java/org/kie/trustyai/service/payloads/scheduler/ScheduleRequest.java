package org.kie.trustyai.service.payloads.scheduler;

import java.util.UUID;

import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;

public class ScheduleRequest {
    public UUID id;
    public ReconciledBaseMetricRequest request;

    public ScheduleRequest() {

    }

    public ScheduleRequest(UUID id, ReconciledBaseMetricRequest request) {
        this.id = id;
        this.request = request;
    }
}
