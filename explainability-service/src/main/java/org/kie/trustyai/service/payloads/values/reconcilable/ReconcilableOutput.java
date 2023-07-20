package org.kie.trustyai.service.payloads.values.reconcilable;

import com.fasterxml.jackson.databind.node.ValueNode;

public class ReconcilableOutput extends ReconcilableField {
    public ReconcilableOutput(ValueNode rawValueNode) {
        super(rawValueNode, "output");
    }
}
