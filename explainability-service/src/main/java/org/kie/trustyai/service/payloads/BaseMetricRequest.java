package org.kie.trustyai.service.payloads;

import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "protected", "favorable" })
public class BaseMetricRequest {

    private String protectedAttribute;

    private TypedValue favorableOutcome;
    private String outcomeName;
    private TypedValue privilegedAttribute;
    private TypedValue unprivilegedAttribute;

    public BaseMetricRequest() {
        // Public default no-argument constructor
    }

    public String getProtectedAttribute() {
        return protectedAttribute;
    }

    public void setProtectedAttribute(String protectedAttribute) {
        this.protectedAttribute = protectedAttribute;
    }

    public TypedValue getFavorableOutcome() {
        return favorableOutcome;
    }

    public void setFavorableOutcome(TypedValue favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public TypedValue getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(TypedValue privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public TypedValue getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(TypedValue unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }
}
