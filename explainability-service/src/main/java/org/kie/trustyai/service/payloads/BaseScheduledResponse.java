package org.kie.trustyai.service.payloads;

import java.util.Date;
import java.util.UUID;

public class BaseScheduledResponse {

    public UUID requestId;
    public Date timestamp;

    public BaseScheduledResponse() {

    }

    public BaseScheduledResponse(UUID uuid) {
        this.requestId = uuid;
        this.timestamp = new Date();
    }
}
