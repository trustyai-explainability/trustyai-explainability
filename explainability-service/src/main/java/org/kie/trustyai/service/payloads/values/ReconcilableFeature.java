package org.kie.trustyai.service.payloads.values;

import com.fasterxml.jackson.databind.node.ValueNode;

import java.util.Optional;


public class ReconcilableFeature {
    private final ValueNode rawValueNode;
    private Optional<TypedValue> typeToReconcile;

    public ReconcilableFeature(ValueNode rawValueNode){
        this.rawValueNode = rawValueNode;
        this.typeToReconcile = Optional.empty();
    }

    public ValueNode getRawValueNode() {
        return rawValueNode;
    }

    public Optional<TypedValue> getTypeToReconcile() {
        return typeToReconcile;
    }

    public void setTypeToReconcile(Optional<TypedValue> typeToReconcile) {
        this.typeToReconcile = typeToReconcile;
    }

    @Override
    public String toString() {
        return rawValueNode.toString();
    }
}
