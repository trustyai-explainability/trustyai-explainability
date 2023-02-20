package org.kie.trustyai.service.endpoints.metrics;

import java.util.HashMap;
import java.util.Map;

public class RequestPayloadGenerator {

    public static Map<String, Object> correct() {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("protectedAttribute", "gender");
        Map<String, Object> favorableOutcome = new HashMap<>();
        favorableOutcome.put("type", "INT32");
        favorableOutcome.put("value", 1);
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
        return payload;
    }

    public static Map<String, Object> incorrectType() {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("protectedAttribute", "gender");
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
        return payload;
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
        return payload;
    }
}
