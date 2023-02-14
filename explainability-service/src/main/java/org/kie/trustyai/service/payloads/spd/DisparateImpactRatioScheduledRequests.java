package org.kie.trustyai.service.payloads.spd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

@Singleton
public class DisparateImpactRatioScheduledRequests {

    private final Map<UUID, GroupStatisticalParityDifferenceRequest> requests = new HashMap<>();

    public Map<UUID, GroupStatisticalParityDifferenceRequest> getRequests() {
        return requests;
    }
}
