package org.kie.trustyai.service.payloads.values.reconcilable;

import com.fasterxml.jackson.databind.node.ValueNode;

public class ReconcilableFeature extends ReconcilableField {
    public ReconcilableFeature(ValueNode rawValueNode) {
        super(rawValueNode, "feature");
    }
}
