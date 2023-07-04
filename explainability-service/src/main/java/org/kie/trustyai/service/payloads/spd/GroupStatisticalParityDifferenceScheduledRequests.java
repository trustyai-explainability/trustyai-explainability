package org.kie.trustyai.service.payloads.spd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;

@Singleton
public class GroupStatisticalParityDifferenceScheduledRequests {

    private final Map<UUID, GroupMetricRequest> requests = new HashMap<>();

    public Map<UUID, GroupMetricRequest> getRequests() {
        return requests;
    }
}
