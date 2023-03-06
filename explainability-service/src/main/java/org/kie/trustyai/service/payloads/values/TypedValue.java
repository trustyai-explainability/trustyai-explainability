package org.kie.trustyai.service.payloads.values;

import com.fasterxml.jackson.databind.JsonNode;

public class TypedValue {
    public DataType type;
    public JsonNode value;

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
