package org.kie.trustyai.service.payloads.values.reconcilable.deserializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
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
    private static final Logger LOG = Logger.getLogger(ReconcilableFeatureDeserializer.class);

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

            if (value.isValueNode()) {
                rf = new ReconcilableFeature((ValueNode) value);
            } else {
                List<ValueNode> valueNodes = new ArrayList<>();
                for (JsonNode subNode : value) {
                    valueNodes.add((ValueNode) subNode);
                }
                rf = new ReconcilableFeature(valueNodes);
            }

            if (!node.get("type").asText().equals("null")) {
                List<TypedValue> tvs = new ArrayList<>();

                // parse as list
                for (ValueNode subNode : rf.getRawValueNodes()) {
                    TypedValue tv = new TypedValue();
                    tv.setValue(subNode);
                    String type = node.get("type").asText();
                    tv.setType(Enum.valueOf(DataType.class, type));
                    tvs.add(tv);
                }
                rf.setReconciledType(Optional.of(tvs));
            } else {
                rf.setReconciledType(Optional.empty());
            }
            return rf;
        } else {
            throw new IllegalArgumentException("Passed JSON Node was not a ReconcilableFeature: no field found with name='type'");
        }

    }
}
