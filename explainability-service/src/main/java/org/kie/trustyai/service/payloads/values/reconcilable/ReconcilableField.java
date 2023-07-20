package org.kie.trustyai.service.payloads.values.reconcilable;

import java.util.Optional;

import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.reconcilable.serializers.ReconcilableFieldSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ValueNode;

@JsonSerialize(using = ReconcilableFieldSerializer.class)
public abstract class ReconcilableField {
    final ValueNode rawValueNode;
    private Optional<TypedValue> reconciledType;
    private final String category;

    protected ReconcilableField(ValueNode rawValueNode, String category) {
        this.rawValueNode = rawValueNode;
        this.reconciledType = Optional.empty();
        this.category = category;
    }

    public ValueNode getRawValueNode() {
        return rawValueNode;
    }

    public Optional<TypedValue> getReconciledType() {
        return reconciledType;
    }

    public void setReconciledType(Optional<TypedValue> reconciledType) {
        this.reconciledType = reconciledType;
    }

    public String getCategory() {
        return category;
    }

    public String toString() {
        return rawValueNode.toString();
    }
}
