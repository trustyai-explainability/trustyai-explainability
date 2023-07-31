package org.kie.trustyai.service.payloads.values.reconcilable.serializers;

import java.io.IOException;

import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableField;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

// this serializers ReconcilableFeatures/Outputs in a way that minimally modifies the existing documented JSON schema"
// ReconcilableField -> {value: x, type: x, category: feature/output}
public class ReconcilableFieldSerializer extends StdSerializer<ReconcilableField> {

    public ReconcilableFieldSerializer() {
        this(null);
    }

    public ReconcilableFieldSerializer(Class<ReconcilableField> t) {
        super(t);
    }

    @Override
    public void serialize(ReconcilableField value, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        jgen.writeStartObject();
        if (value.getReconciledType().isPresent()) {
            DataType dt = value.getReconciledType().get().getType();
            jgen.writeStringField("type", dt.toString());
        } else {
            jgen.writeStringField("type", "null");
        }
        jgen.writeObjectField("value", value.getRawValueNode());
        jgen.writeEndObject();

    }
}
