package org.kie.trustyai.connectors.kserve.v1;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KServeV1ResponsePayload {
    @JsonProperty("predictions")
    private List<Object> predictions;

    public List<Object> getPredictions() {
        return predictions;
    }

    public KServeV1ResponsePayload() {
        // NO-OP
    }

    public void setPredictions(List<Object> predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        return "KServeV1ResponsePayload{" +
                "predictions=" + predictions +
                '}';
    }
}
