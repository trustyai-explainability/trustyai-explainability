package org.kie.trustyai.service.payloads.dir;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.kie.trustyai.service.payloads.BaseMetricRequest;

@Singleton
public class DisparateImpactRatioScheduledRequests {

    private final Map<UUID, BaseMetricRequest> requests = new HashMap<>();

    public Map<UUID, BaseMetricRequest> getRequests() {
        return requests;
    }
}
