package org.kie.trustyai.service.payloads;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.DataType;
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
        typedValue.type = DataType.BOOL;
        typedValue.value = actualObj.get("bool");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Boolean);
        assertTrue((Boolean) value.getUnderlyingObject());
    }

    @Test
    void convertToFloatValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = DataType.FLOAT;
        typedValue.value = actualObj.get("float");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Double);
        assertEquals(1.2, value.getUnderlyingObject());
    }

    @Test
    void convertToDoubleValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = DataType.DOUBLE;
        typedValue.value = actualObj.get("double");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Double);
        assertEquals(1.0, value.getUnderlyingObject());
    }

    @Test
    void convertToIntValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = DataType.INT32;
        typedValue.value = actualObj.get("int");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Integer);
        assertEquals(1, value.getUnderlyingObject());
    }

    @Test
    void convertToLongValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = DataType.INT64;
        typedValue.value = actualObj.get("long");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof Long);
        assertEquals(2L, value.getUnderlyingObject());
    }

    @Test
    void convertToStringValue() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode actualObj = mapper.readTree(jsonValues);

        final TypedValue typedValue = new TypedValue();
        typedValue.type = DataType.STRING;
        typedValue.value = actualObj.get("string");

        final Value value = PayloadConverter.convertToValue(typedValue);

        assertTrue(value.getUnderlyingObject() instanceof String);
        assertEquals("string", value.getUnderlyingObject());
    }

    @Test
    void convertToType() {
        assertEquals(Type.BOOLEAN, PayloadConverter.convertToType(DataType.BOOL));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType(DataType.FLOAT));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType(DataType.DOUBLE));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType(DataType.INT32));
        assertEquals(Type.NUMBER, PayloadConverter.convertToType(DataType.INT64));
        assertEquals(Type.CATEGORICAL, PayloadConverter.convertToType(DataType.STRING));
    }
}