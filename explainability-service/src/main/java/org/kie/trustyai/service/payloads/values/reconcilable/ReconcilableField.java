package org.kie.trustyai.service.payloads.values.reconcilable;

import java.util.Optional;

import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.reconcilable.serializers.ReconcilableFieldSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ValueNode;

public abstract class ReconcilableField {
    final ValueNode rawValueNode;
    private Optional<TypedValue> reconciledType;

    protected ReconcilableField(ValueNode rawValueNode) {
        this.rawValueNode = rawValueNode;
        this.reconciledType = Optional.empty();
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

    public String toString() {
        return this.getRawValueNode().toString();
    }
}
