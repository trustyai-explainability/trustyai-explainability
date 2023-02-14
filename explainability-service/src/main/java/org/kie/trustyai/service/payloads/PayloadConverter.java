package org.kie.trustyai.service.payloads;

import org.kie.trustyai.explainability.model.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class PayloadConverter {
    public static Value convertToValue(JsonNode node) {
        JsonNodeType type = node.getNodeType();
        if (type == JsonNodeType.BOOLEAN) {
            return new Value(node.asBoolean());
        } else if (type == JsonNodeType.NUMBER) {
            if (node.isInt()) {
                return new Value(node.asInt());
            } else {
                return new Value(node.asDouble());
            }
        } else if (type == JsonNodeType.STRING) {
            return new Value(node.asText());
        } else {
            return new Value(null);
        }
    }

}
