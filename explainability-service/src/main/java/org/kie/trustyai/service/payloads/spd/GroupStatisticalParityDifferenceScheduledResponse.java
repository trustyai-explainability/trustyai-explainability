package org.kie.trustyai.service.payloads.spd;

import java.util.Date;
import java.util.UUID;

public class GroupStatisticalParityDifferenceScheduledResponse {

    public UUID requestId;
    public Date timestamp = new Date();

    public GroupStatisticalParityDifferenceScheduledResponse(UUID uuid) {
        this.requestId = uuid;
    }
}
