package org.kie.trustyai.service.payloads.values.reconcilable;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.node.ValueNode;

public abstract class ReconcilableField {
    private static final Logger LOG = Logger.getLogger(ReconcilableField.class);

    private final List<ValueNode> rawValueNodes;
    private final ValueNode rawValueNode;
    private Optional<List<TypedValue>> reconciledType;

    protected ReconcilableField(ValueNode rawValueNode) {
        this.rawValueNode = rawValueNode;
        this.rawValueNodes = null;
        this.reconciledType = Optional.empty();
    }

    // disable until better integrated with ODH UI
    //    protected ReconcilableField(List<ValueNode> rawValueNode) {
    //        this.rawValueNodes = rawValueNode;
    //        this.rawValueNode = null;
    //        this.reconciledType = Optional.empty();
    //    }

    public List<ValueNode> getRawValueNodes() {
        if (isMultipleValued()) {
            return rawValueNodes;
        } else {
            return List.of(rawValueNode);
        }
    }

    public ValueNode getRawValueNode() {
        if (!isMultipleValued()) {
            return rawValueNode;
        } else {
            throw new IllegalArgumentException("Cannot return single value of multiple-valued ReconcilableField");
        }
    }

    public boolean isMultipleValued() {
        return rawValueNodes != null;
    }

    public Optional<List<TypedValue>> getReconciledType() {
        return reconciledType;
    }

    public void setReconciledType(Optional<List<TypedValue>> reconciledType) {
        this.reconciledType = reconciledType;
    }

    public String toString() {
        if (isMultipleValued()) {
            return rawValueNodes.toString();
        } else {
            return rawValueNode.toString();
        }
    }
}
