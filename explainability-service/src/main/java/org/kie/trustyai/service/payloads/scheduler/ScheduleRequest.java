package org.kie.trustyai.service.payloads.scheduler;

import java.util.UUID;

import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

public class ScheduleRequest {
    public UUID id;
    public ReconciledGroupMetricRequest request;

    public ScheduleRequest() {

    }

    public ScheduleRequest(UUID id, ReconciledGroupMetricRequest request) {
        this.id = id;
        this.request = request;
    }
}
