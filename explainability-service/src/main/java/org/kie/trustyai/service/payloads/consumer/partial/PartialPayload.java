package org.kie.trustyai.service.payloads.consumer.partial;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(PartialPayloadId.class)
public abstract class PartialPayload implements PartialPayloadInterface {

    @Id
    private String id;

    @Id
    private PartialKind kind;

    @JdbcTypeCode(SqlTypes.JSON)
    protected Map<String, String> metadata = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public PartialKind getKind() {
        return this.kind;
    }

    public void setKind(PartialKind kind) {
        this.kind = kind;
    }
}
