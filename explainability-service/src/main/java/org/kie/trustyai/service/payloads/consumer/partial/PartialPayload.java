package org.kie.trustyai.service.payloads.consumer.partial;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public abstract class PartialPayload implements PartialPayloadInterface {
    @EmbeddedId
    private PartialPayloadId partialPayloadId = new PartialPayloadId();

    @ElementCollection
    @Column(columnDefinition = "text")
    protected Map<String, String> metadata = new HashMap<>();

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getId() {
        return partialPayloadId.getPredictionId();
    }

    @Override
    public void setId(String id) {
        this.partialPayloadId.setPredictionId(id);
    }

    @Override
    public PartialKind getKind() {
        return partialPayloadId.getKind();
    }

    @Override
    public void setKind(PartialKind kind) {
        this.partialPayloadId.setKind(kind);
    }
}
