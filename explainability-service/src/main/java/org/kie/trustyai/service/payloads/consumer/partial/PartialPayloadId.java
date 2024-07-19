package org.kie.trustyai.service.payloads.consumer.partial;

import java.io.Serializable;
import java.util.Objects;

public class PartialPayloadId implements Serializable {
    private String id;
    private PartialKind kind;

    public PartialPayloadId() {
    }

    public PartialPayloadId(String id, PartialKind kind) {
        this.id = id;
        this.kind = kind;
    }

    public void setId(String id) {
        this.id = id;
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
        return Objects.equals(id, that.id) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind);
    }

    public static PartialPayloadId request(String id) {
        return new PartialPayloadId(id, PartialKind.request);
    }

    public static PartialPayloadId response(String id) {
        return new PartialPayloadId(id, PartialKind.response);
    }
}
