package org.kie.trustyai.service.payloads.values.reconcilable.deserializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

public class ValueNodeDeserializer extends StdDeserializer<ValueNode> {
    public ValueNodeDeserializer() {
        this(null);
    }

    public ValueNodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ValueNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return (ValueNode) node.get("rawValueNode");
    }
}
