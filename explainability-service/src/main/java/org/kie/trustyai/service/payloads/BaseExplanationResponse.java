package org.kie.trustyai.service.payloads;

import java.util.Date;

public class BaseExplanationResponse {

    public final Date timestamp = new Date();

    protected String type = "explanation";

    protected String id;

    public BaseExplanationResponse() {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
