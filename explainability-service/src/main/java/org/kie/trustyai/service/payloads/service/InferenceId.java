package org.kie.trustyai.service.payloads.service;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceId {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;

    public InferenceId(String id, LocalDateTime timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InferenceId that = (InferenceId) o;
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
        return "InferenceId{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
