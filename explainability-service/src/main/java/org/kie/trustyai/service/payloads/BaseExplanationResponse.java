package org.kie.trustyai.service.payloads;

import java.util.Date;
import java.util.UUID;

public class BaseExplanationResponse {

    public final Date timestamp = new Date();

    protected String type = "explanation";

    protected UUID id;

    public BaseExplanationResponse() {
        this.id = UUID.randomUUID();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
