package org.kie.trustyai.service.payloads.values;

import com.fasterxml.jackson.databind.node.ValueNode;
import java.util.Optional;


public class ReconcilableOutput {
    private final ValueNode rawValueNode;
    private Optional<TypedValue> typeToReconcile;

    public ReconcilableOutput(ValueNode rawValueNode){
        this.rawValueNode = rawValueNode;
        this.typeToReconcile = null;
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