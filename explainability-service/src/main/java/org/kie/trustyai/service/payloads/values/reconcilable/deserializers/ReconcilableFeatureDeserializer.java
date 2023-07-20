package org.kie.trustyai.service.payloads.values.reconcilable.deserializers;

import java.io.IOException;
import java.util.Optional;

import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

// deserializers ReconcilableFields; is the inverse of the ReconcilableFieldSerializer
public class ReconcilableFeatureDeserializer extends StdDeserializer<ReconcilableFeature> {
    public ReconcilableFeatureDeserializer() {
        this(null);
    }

    public ReconcilableFeatureDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ReconcilableFeature deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        ReconcilableFeature rf;
        if (node.has("type")) {
            JsonNode value = node.get("value");
            rf = new ReconcilableFeature((ValueNode) value);

            if (!node.get("type").asText().equals("null")) {
                String type = node.get("type").asText();
                TypedValue tv = new TypedValue();
                tv.setValue(value);
                tv.setType(Enum.valueOf(DataType.class, type));
                rf.setReconciledType(Optional.of(tv));
            } else {
                rf.setReconciledType(Optional.empty());
            }
        } else {
            rf = new ReconcilableFeature((ValueNode) node);
        }
        return rf;
    }
}
