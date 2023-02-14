package org.kie.trustyai.service.payloads.spd;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

@JsonPropertyOrder({ "protected", "favorable" })
public class GroupStatisticalParityDifferenceRequest {

    private String protectedAttribute;

    private JsonNode favorableOutcome;
    private String outcomeName;
    private JsonNode privilegedAttribute;
    private JsonNode unprivilegedAttribute;

    public GroupStatisticalParityDifferenceRequest() {

    }

    public String getProtectedAttribute() {
        return protectedAttribute;
    }

    public void setProtectedAttribute(String protectedAttribute) {
        this.protectedAttribute = protectedAttribute;
    }

    public JsonNode getFavorableOutcome() {
        return favorableOutcome;
    }

    public void setFavorableOutcome(JsonNode favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public JsonNode getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(JsonNode privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public JsonNode getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(JsonNode unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }
}
