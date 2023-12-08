package org.kie.trustyai.service.payloads.values.reconcilable;

import java.util.List;

import org.kie.trustyai.service.payloads.values.reconcilable.deserializers.ReconcilableFeatureDeserializer;
import org.kie.trustyai.service.payloads.values.reconcilable.serializers.ReconcilableFieldSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ValueNode;

@JsonDeserialize(using = ReconcilableFeatureDeserializer.class)
@JsonSerialize(using = ReconcilableFieldSerializer.class)
public class ReconcilableFeature extends ReconcilableField {
    public ReconcilableFeature(ValueNode rawValueNode) {
        super(rawValueNode);
    }

    public ReconcilableFeature(List<ValueNode> rawValueNode) {
        super(rawValueNode);
    }
}
