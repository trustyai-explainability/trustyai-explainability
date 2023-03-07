package org.kie.trustyai.service.payloads;

import java.util.Date;
import java.util.UUID;

public class BaseScheduledResponse {

    private UUID requestId;
    private Date timestamp;

    public BaseScheduledResponse() {

    }

    public BaseScheduledResponse(UUID uuid) {
        this.requestId = uuid;
        this.timestamp = new Date();
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
