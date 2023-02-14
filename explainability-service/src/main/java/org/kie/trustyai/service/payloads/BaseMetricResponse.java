package org.kie.trustyai.service.payloads;

import java.util.Date;
import java.util.UUID;

public abstract class BaseMetricResponse {
    public String type = "metric";
    public String name;
    public Double value;
    public UUID id;
    public Date timestamp = new Date();

    public BaseMetricResponse(Double value) {
        this.value = value;
        this.id = UUID.randomUUID();
    }

}
