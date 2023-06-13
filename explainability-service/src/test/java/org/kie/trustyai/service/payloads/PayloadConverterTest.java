package org.kie.trustyai.service.payloads;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

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
        ValueNode boolNode = JsonNodeFactory.instance.booleanNode(true);
        ValueNode nonBoolNode = JsonNodeFactory.instance.textNode("true");

        assertTrue(PayloadConverter.checkValueType(DataType.BOOL, boolNode));
        assertFalse(PayloadConverter.checkValueType(DataType.BOOL, nonBoolNode));
    }

    @Test
    void testCheckValueTypeFloat() {
        ValueNode node1 = JsonNodeFactory.instance.numberNode(1.1f);
        ValueNode node2 = JsonNodeFactory.instance.numberNode(1); // edge case
        ValueNode node3 = JsonNodeFactory.instance.numberNode(1.1d);
        ValueNode node4 = JsonNodeFactory.instance.numberNode(1d);
        ValueNode nonFloatNode = JsonNodeFactory.instance.textNode("1.1");

        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, node1));
        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, node2)); // edge case
        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, node3));
        assertTrue(PayloadConverter.checkValueType(DataType.FLOAT, node4));
        assertFalse(PayloadConverter.checkValueType(DataType.FLOAT, nonFloatNode));
    }

    @Test
    void testCheckValueTypeDouble() {

        ValueNode node1 = DoubleNode.valueOf(1.1);
        ValueNode node2 = IntNode.valueOf(1);
        ValueNode nonDoubleNode = TextNode.valueOf("1.1");
        ValueNode node3 = FloatNode.valueOf(1.1f);
        ValueNode node4 = FloatNode.valueOf(1f);

        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, node1));
        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, node2));
        assertFalse(PayloadConverter.checkValueType(DataType.DOUBLE, nonDoubleNode));
        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, node3));
        assertTrue(PayloadConverter.checkValueType(DataType.DOUBLE, node4));
    }

    @Test
    void testCheckValueTypeInt32() {

        ValueNode node1 = FloatNode.valueOf(1.1f);
        ValueNode node2 = IntNode.valueOf(1);
        ValueNode nonInt32Node = TextNode.valueOf("1.1");
        ValueNode node3 = DoubleNode.valueOf(1.1d);
        ValueNode node4 = DoubleNode.valueOf(1d);
        ValueNode node5 = FloatNode.valueOf(1f);

        assertFalse(PayloadConverter.checkValueType(DataType.INT32, node1));
        assertTrue(PayloadConverter.checkValueType(DataType.INT32, node2));
        assertFalse(PayloadConverter.checkValueType(DataType.INT32, nonInt32Node));
        assertFalse(PayloadConverter.checkValueType(DataType.INT32, node3));
        assertTrue(PayloadConverter.checkValueType(DataType.INT32, node4));
        assertTrue(PayloadConverter.checkValueType(DataType.INT32, node5));
    }

    @Test
    void testCheckValueTypeInt64() {
        ValueNode node1 = FloatNode.valueOf(1.1f);
        ValueNode node2 = IntNode.valueOf(1);
        ValueNode nonInt64Node = TextNode.valueOf("1.1");
        ValueNode node3 = DoubleNode.valueOf(1.1d);
        ValueNode node4 = DoubleNode.valueOf(1d);
        ValueNode node5 = FloatNode.valueOf(1f);

        assertFalse(PayloadConverter.checkValueType(DataType.INT64, node1));
        assertTrue(PayloadConverter.checkValueType(DataType.INT64, node2));
        assertFalse(PayloadConverter.checkValueType(DataType.INT64, nonInt64Node));
        assertFalse(PayloadConverter.checkValueType(DataType.INT64, node3));
        assertTrue(PayloadConverter.checkValueType(DataType.INT64, node4));
        assertTrue(PayloadConverter.checkValueType(DataType.INT64, node5));

    }

    @Test
    void testCheckValueTypeUnknown() {
        ValueNode node = JsonNodeFactory.instance.textNode("unknown");

        assertFalse(PayloadConverter.checkValueType(DataType.UNKNOWN, node));
    }

}