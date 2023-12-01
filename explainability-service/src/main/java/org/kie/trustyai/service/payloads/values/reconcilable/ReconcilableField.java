package org.kie.trustyai.service.payloads.values.reconcilable;

import java.util.List;
import java.util.Optional;

import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.node.ValueNode;

public abstract class ReconcilableField {
    private final List<ValueNode> rawValueNodes;
    private Optional<List<TypedValue>> reconciledType;

    protected ReconcilableField(ValueNode rawValueNode) {
        this.rawValueNodes = List.of(rawValueNode);
        this.reconciledType = Optional.empty();
    }

    protected ReconcilableField(List<ValueNode> rawValueNode) {
        this.rawValueNodes = rawValueNode;
        this.reconciledType = Optional.empty();
    }

    public List<ValueNode> getRawValueNodes() {
        return rawValueNodes;
    }

    public Optional<List<TypedValue>> getReconciledType() {
        return reconciledType;
    }

    public void setReconciledType(Optional<List<TypedValue>> reconciledType) {
        this.reconciledType = reconciledType;
    }

    public String toString() {
        return this.getRawValueNodes().toString();
    }
}
