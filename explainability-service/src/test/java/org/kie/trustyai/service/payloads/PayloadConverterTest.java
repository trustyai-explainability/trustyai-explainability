package org.kie.trustyai.service.payloads;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testCheckValueTypeBool() {
        JsonNode boolNode = JsonNodeFactory.instance.booleanNode(true);
        JsonNode nonBoolNode = JsonNodeFactory.instance.textNode("true");

        assertTrue(PayloadConverter.checkValueType(DataType.BOOL, boolNode));
        assertFalse(PayloadConverter.checkValueType(DataType.BOOL, nonBoolNode));
    }

    @Test
    void testCheckValueTypeFloat() {
        JsonNode floatNode1 = JsonNodeFactory.instance.numberNode(1.1f);
        JsonNode floatNode2 = JsonNodeFactory.instance.numberNode(1); // edge case
        JsonNode nonFloatNode = JsonNodeFactory.instance.textNode("1.1");

        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, floatNode1));
        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, floatNode2)); // edge case
        assertFalse(PayloadConverter.checkValueType(DataType.FLOAT, nonFloatNode));
    }

    @Test
    void testCheckValueTypeDouble() {
        JsonNode doubleNode1 = JsonNodeFactory.instance.numberNode(1.1f);
        JsonNode doubleNode2 = JsonNodeFactory.instance.numberNode(1);
        JsonNode nonFloatNode = JsonNodeFactory.instance.textNode("1.1");

        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, doubleNode1));
        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, doubleNode2));
        assertFalse(PayloadConverter.checkValueType(DataType.DOUBLE, nonFloatNode));
    }

    @Test
    void testCheckValueTypeInt32() {
        JsonNode int32Node1 = JsonNodeFactory.instance.numberNode(1.1f);
        JsonNode int32Node2 = JsonNodeFactory.instance.numberNode(1);
        JsonNode nonInt32Node = JsonNodeFactory.instance.textNode("1.1");

        assertFalse(PayloadConverter.checkValueType(DataType.INT32, int32Node1));
        assertTrue(PayloadConverter.checkValueType(DataType.INT32, int32Node2));
        assertFalse(PayloadConverter.checkValueType(DataType.INT32, nonInt32Node));
    }

    @Test
    void testCheckValueTypeInt64() {
        JsonNode int64Node1 = JsonNodeFactory.instance.numberNode(1.1f);
        JsonNode int64Node2 = JsonNodeFactory.instance.numberNode(1);
        JsonNode nonInt64Node = JsonNodeFactory.instance.textNode("1.1");

        assertFalse(PayloadConverter.checkValueType(DataType.INT64, int64Node1));
        assertTrue(PayloadConverter.checkValueType(DataType.INT64, int64Node2));
        assertFalse(PayloadConverter.checkValueType(DataType.INT64, nonInt64Node));
    }

    @Test
    void testCheckValueTypeUnknown() {
        JsonNode node = JsonNodeFactory.instance.textNode("unknown");

        assertFalse(PayloadConverter.checkValueType(DataType.UNKNOWN, node));
    }

    @Test
    void testCheckValueTypeMap() {
        // Map is not covered in your function, it will always return false
        JsonNode node = JsonNodeFactory.instance.objectNode().put("key", "value");

        assertFalse(PayloadConverter.checkValueType(DataType.MAP, node));

    }
}