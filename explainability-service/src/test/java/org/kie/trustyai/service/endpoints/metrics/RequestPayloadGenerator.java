package org.kie.trustyai.service.endpoints.metrics;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.BaseMetricRequest;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class RequestPayloadGenerator {

    private static final String MODEL_ID = "example1";

    public static BaseMetricRequest correct() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(IntNode.valueOf(1));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(IntNode.valueOf(1));
        request.setUnprivilegedAttribute(IntNode.valueOf(0));
        request.setModelId(MODEL_ID);

        return request;
    }

    public static BaseMetricRequest named(String name) {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(IntNode.valueOf(1));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(IntNode.valueOf(1));
        request.setUnprivilegedAttribute(IntNode.valueOf(0));
        request.setModelId(MODEL_ID);
        request.setRequestName(name);

        return request;
    }

    public static BaseMetricRequest incorrectType() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(TextNode.valueOf("male"));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(IntNode.valueOf(1));
        request.setUnprivilegedAttribute(IntNode.valueOf(0));
        request.setModelId(MODEL_ID);

        return request;
    }

    public static BaseMetricRequest incorrectInput() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("city");
        request.setFavorableOutcome(TextNode.valueOf("approved"));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(IntNode.valueOf(1));
        request.setUnprivilegedAttribute(IntNode.valueOf(0));
        request.setModelId(MODEL_ID);
        return request;
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
