package org.kie.trustyai.service.payloads;

import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;

import static org.kie.trustyai.service.payloads.values.DataType.*;

public class PayloadConverter {
    private PayloadConverter() {
    }

    public static Value convertToValue(TypedValue node) {
        final DataType type = node.getType();
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

    public static Type convertToType(DataType type) {
        try {
            if (type == BOOL) {
                return Type.BOOLEAN;
            } else if (type == FLOAT || type == DOUBLE || type == INT32 || type == INT64) {
                return Type.NUMBER;
            } else if (type == STRING) {
                return Type.CATEGORICAL;
            } else {
                return Type.UNDEFINED;
            }
        } catch (IllegalArgumentException e) {
            return Type.UNDEFINED;
        }
    }

}
