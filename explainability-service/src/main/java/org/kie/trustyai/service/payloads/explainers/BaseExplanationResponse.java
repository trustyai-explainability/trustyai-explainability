package org.kie.trustyai.service.payloads.explainers;

import java.util.Date;

public class BaseExplanationResponse {

    public final Date timestamp = new Date();

    protected String type = "explanation";

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

}
