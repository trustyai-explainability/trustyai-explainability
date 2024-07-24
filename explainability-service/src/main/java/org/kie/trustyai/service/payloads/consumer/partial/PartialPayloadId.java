package org.kie.trustyai.service.payloads.consumer.partial;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class PartialPayloadId implements Serializable {
    private String predictionId;
    private PartialKind kind;

    public PartialPayloadId() {
    }

    public PartialPayloadId(String predictionId, PartialKind kind) {
        this.predictionId = predictionId;
        this.kind = kind;
    }

    public PartialKind getKind() {
        return kind;
    }

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String id) {
        this.predictionId = id;
    }

    public void setKind(PartialKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PartialPayloadId))
            return false;
        PartialPayloadId that = (PartialPayloadId) o;
        return Objects.equals(predictionId, that.predictionId) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predictionId, kind);
    }

    public static PartialPayloadId request(String id) {
        return new PartialPayloadId(id, PartialKind.request);
    }

    public static PartialPayloadId response(String id) {
        return new PartialPayloadId(id, PartialKind.response);
    }
}
