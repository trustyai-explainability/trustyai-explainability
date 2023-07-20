package org.kie.trustyai.service.payloads.values.reconcilable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.kie.trustyai.service.payloads.values.reconcilable.deserializers.ReconcilableOutputDeserializer;
import org.kie.trustyai.service.payloads.values.reconcilable.serializers.ReconcilableFieldSerializer;

@JsonDeserialize(using = ReconcilableOutputDeserializer.class)
@JsonSerialize(using = ReconcilableFieldSerializer.class)
public class ReconcilableOutput extends ReconcilableField {
    public ReconcilableOutput(ValueNode rawValueNode) {
        super(rawValueNode);
    }
}
