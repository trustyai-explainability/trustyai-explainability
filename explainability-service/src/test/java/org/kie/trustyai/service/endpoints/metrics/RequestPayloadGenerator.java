package org.kie.trustyai.service.endpoints.metrics;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static org.kie.trustyai.service.payloads.values.DataType.INT32;
import static org.kie.trustyai.service.payloads.values.DataType.STRING;

public class RequestPayloadGenerator {

    private static final String MODEL_ID = "example1";

    public static BaseMetricRequest correct() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");

        TypedValue favorableOutcome = new TypedValue();
        favorableOutcome.setType(INT32);
        favorableOutcome.setValue(JsonNodeFactory.instance.numberNode(1));
        request.setFavorableOutcome(favorableOutcome);

        request.setOutcomeName("income");

        TypedValue privilegedAttribute = new TypedValue();
        privilegedAttribute.setType(INT32);
        privilegedAttribute.setValue(JsonNodeFactory.instance.numberNode(1));
        request.setPrivilegedAttribute(privilegedAttribute);

        TypedValue unprivilegedAttribute = new TypedValue();
        unprivilegedAttribute.setType(INT32);
        unprivilegedAttribute.setValue(JsonNodeFactory.instance.numberNode(0));
        request.setUnprivilegedAttribute(unprivilegedAttribute);

        request.setModelId(MODEL_ID);

        return request;
    }

    public static BaseMetricRequest incorrectType() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");

        TypedValue favorableOutcome = new TypedValue();
        favorableOutcome.setType(STRING);
        favorableOutcome.setValue(JsonNodeFactory.instance.numberNode(1));
        request.setFavorableOutcome(favorableOutcome);

        request.setOutcomeName("income");

        TypedValue privilegedAttribute = new TypedValue();
        privilegedAttribute.setType(INT32);
        privilegedAttribute.setValue(JsonNodeFactory.instance.numberNode(1));
        request.setPrivilegedAttribute(privilegedAttribute);

        TypedValue unprivilegedAttribute = new TypedValue();
        unprivilegedAttribute.setType(INT32);
        unprivilegedAttribute.setValue(JsonNodeFactory.instance.numberNode(0));
        request.setUnprivilegedAttribute(unprivilegedAttribute);

        request.setModelId(MODEL_ID);

        return request;
    }

    public static Map<String, Object> incorrectInput() {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("protectedAttribute", "city");
        Map<String, Object> favorableOutcome = new HashMap<>();
        favorableOutcome.put("type", "STRING");
        favorableOutcome.put("value", "approved");
        payload.put("favorableOutcome", favorableOutcome);
        payload.put("outcomeName", "income");
        Map<String, Object> privilegedAttribute = new HashMap<>();
        privilegedAttribute.put("type", "INT32");
        privilegedAttribute.put("value", 1);
        payload.put("privilegedAttribute", privilegedAttribute);
        Map<String, Object> unprivilegedAttribute = new HashMap<>();
        unprivilegedAttribute.put("type", "INT32");
        unprivilegedAttribute.put("value", 0);
        payload.put("unprivilegedAttribute", unprivilegedAttribute);

        payload.put("modelId", MODEL_ID);

        return payload;
    }

    public static Map<String, Object> unknownType() {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("protectedAttribute", "city");
        Map<String, Object> favorableOutcome = new HashMap<>();
        favorableOutcome.put("type", "FOO");
        favorableOutcome.put("value", "approved");
        payload.put("favorableOutcome", favorableOutcome);
        payload.put("outcomeName", "income");
        Map<String, Object> privilegedAttribute = new HashMap<>();
        privilegedAttribute.put("type", "INT32");
        privilegedAttribute.put("value", 1);
        payload.put("privilegedAttribute", privilegedAttribute);
        Map<String, Object> unprivilegedAttribute = new HashMap<>();
        unprivilegedAttribute.put("type", "INT32");
        unprivilegedAttribute.put("value", 0);
        payload.put("unprivilegedAttribute", unprivilegedAttribute);

        payload.put("modelId", MODEL_ID);

        return payload;
    }
}
