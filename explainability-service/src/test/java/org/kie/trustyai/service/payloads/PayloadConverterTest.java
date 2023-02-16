package org.kie.trustyai.service.payloads;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadConverterTest {

    private static final String jsonValues = "{\"bool\": true,\"float\": 1.2," +
            "\"double\": 1.0, \"int\": 1, \"long\": 2, \"string\": \"string\"}";

    @Test
    void convertToBoolValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "BOOL";
        typedValue.value = actualObj.get("bool");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Boolean);
    }

    @Test
    void convertToFloatValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "FLOAT";
        typedValue.value = actualObj.get("float");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Double);
    }

    @Test
    void convertToDoubleValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "DOUBLE";
        typedValue.value = actualObj.get("double");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Double);
    }

    @Test
    void convertToIntValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "INT32";
        typedValue.value = actualObj.get("int");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Integer);
    }

    @Test
    void convertToLongValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "INT64";
        typedValue.value = actualObj.get("long");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Long);
    }

    @Test
    void convertToStringValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = "STRING";
        typedValue.value = actualObj.get("string");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof String);
    }

    @Test
    void convertToType() {
        assertEquals(Type.BOOLEAN, PayloadConverter.convertToType("BOOL"));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType("FLOAT"));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType("DOUBLE"));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType("INT32"));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType("INT64"));
        assertEquals(Type.CATEGORICAL, PayloadConverter.convertToType("STRING"));
        assertEquals(Type.UNDEFINED, PayloadConverter.convertToType("FOO"));
    }
}