package org.kie.trustyai.service.endpoints.metrics;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.metrics.identity.IdentityMetricRequest;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class RequestPayloadGenerator {

    private static final String MODEL_ID = "example1";

    public static GroupMetricRequest correct() {
        GroupMetricRequest request = new GroupMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(new ReconcilableOutput(IntNode.valueOf(1)));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(1)));
        request.setUnprivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(0)));
        request.setModelId(MODEL_ID);

        return request;
    }

    public static GroupMetricRequest named(String name) {
        GroupMetricRequest request = new GroupMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(new ReconcilableOutput(IntNode.valueOf(1)));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(1)));
        request.setUnprivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(0)));
        request.setModelId(MODEL_ID);
        request.setRequestName(name);

        return request;
    }

    public static GroupMetricRequest incorrectType() {
        GroupMetricRequest request = new GroupMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(new ReconcilableOutput(TextNode.valueOf("male")));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(1)));
        request.setUnprivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(0)));
        request.setModelId(MODEL_ID);

        return request;
    }

    public static GroupMetricRequest incorrectInput() {
        GroupMetricRequest request = new GroupMetricRequest();
        request.setProtectedAttribute("city");
        request.setFavorableOutcome(new ReconcilableOutput(TextNode.valueOf("approved")));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(1)));
        request.setUnprivilegedAttribute(new ReconcilableFeature(IntNode.valueOf(0)));
        request.setModelId(MODEL_ID);
        return request;
    }

    public static IdentityMetricRequest correctIdentityInput() {
        IdentityMetricRequest request = new IdentityMetricRequest();
        request.setColumnName("gender");
        request.setModelId(MODEL_ID);
        return request;
    }

    public static IdentityMetricRequest incorrectIdentityInput() {
        IdentityMetricRequest request = new IdentityMetricRequest();
        request.setColumnName("THIS_FIELD_DOES_NOT_EXIST");
        request.setModelId(MODEL_ID);
        return request;
    }

    public static BaseMetricRequest incorrectManyWrongNames() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("city");
        request.setFavorableOutcome(TextNode.valueOf("approved"));
        request.setOutcomeName("icnome");
        request.setPrivilegedAttribute(IntNode.valueOf(1));
        request.setUnprivilegedAttribute(IntNode.valueOf(0));
        request.setModelId(MODEL_ID);
        return request;
    }

    public static BaseMetricRequest incorrectManyWrongTypes() {
        BaseMetricRequest request = new BaseMetricRequest();
        request.setProtectedAttribute("gender");
        request.setFavorableOutcome(TextNode.valueOf("approved-doesnt-exist"));
        request.setOutcomeName("income");
        request.setPrivilegedAttribute(TextNode.valueOf("lemons"));
        request.setUnprivilegedAttribute(DoubleNode.valueOf(1.5));
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
