package org.kie.trustyai.service.payloads;

import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.Values;

import static org.kie.trustyai.service.payloads.values.Values.*;

public class PayloadConverter {
    private PayloadConverter() {
    }

    public static Value convertToValue(TypedValue node) {
        final Values type = Values.valueOf(node.getType());
        if (type == BOOL) {
            return new Value(node.getValue().asBoolean());
        } else if (type == FLOAT || type == DOUBLE) {
            return new Value(node.getValue().asDouble());
        } else if (type == INT32) {
            return new Value(node.getValue().asInt());
        } else if (type == INT64) {
            return new Value(node.getValue().asLong());
        } else if (type == STRING) {
            return new Value(node.getValue().asText());
        } else {
            return new Value(null);
        }
    }

}