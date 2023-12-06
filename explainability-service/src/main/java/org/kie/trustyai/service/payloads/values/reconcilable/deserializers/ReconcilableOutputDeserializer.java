package org.kie.trustyai.service.payloads.values.reconcilable.deserializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

// deserializers ReconcilableFields; is the inverse of the ReconcilableFieldSerializer
public class ReconcilableOutputDeserializer extends StdDeserializer<ReconcilableOutput> {
    private static final Logger LOG = Logger.getLogger(ReconcilableOutputDeserializer.class);

    public ReconcilableOutputDeserializer() {
        this(null);
    }

    public ReconcilableOutputDeserializer(Class<?> vc) {
        super(vc);
    }

    private ReconcilableOutput processNode(JsonNode value) {
        if (value.isValueNode()) {
            return new ReconcilableOutput((ValueNode) value);
        } else {
            List<ValueNode> valueNodes = new ArrayList<>();
            for (JsonNode subNode : value) {
                valueNodes.add((ValueNode) subNode);
            }
            return new ReconcilableOutput(valueNodes);
        }
    }

    @Override
    public ReconcilableOutput deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        ReconcilableOutput rf;
        if (node.has("type")) {
            JsonNode value = node.get("value");

            rf = processNode(value);

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
            rf = processNode(node);
        }
        return rf;
    }
}
