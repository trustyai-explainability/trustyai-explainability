package org.kie.trustyai.service.payloads.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class PredictionId {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;

    public PredictionId(String id, LocalDateTime timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PredictionId that = (PredictionId) o;
        return Objects.equals(id, that.id) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PredictionId{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
