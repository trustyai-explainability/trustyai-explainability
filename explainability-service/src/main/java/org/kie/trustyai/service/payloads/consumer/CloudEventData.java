package org.kie.trustyai.service.payloads.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudEventData {

    @JsonProperty("InferenceServiceAttr")
    private String inferenceServiceAttr;

    @JsonProperty("NamespaceAttr")
    private String namespaceAttr;

    @JsonProperty("ComponentAttr")
    private String componentAttr;

    @JsonProperty("EndpointAttr")
    private String endpointAttr;

    public String getInferenceServiceAttr() {
        return inferenceServiceAttr;
    }

    public void setInferenceServiceAttr(String inferenceServiceAttr) {
        this.inferenceServiceAttr = inferenceServiceAttr;
    }

    public String getNamespaceAttr() {
        return namespaceAttr;
    }

    public void setNamespaceAttr(String namespaceAttr) {
        this.namespaceAttr = namespaceAttr;
    }

    public String getComponentAttr() {
        return componentAttr;
    }

    public void setComponentAttr(String componentAttr) {
        this.componentAttr = componentAttr;
    }

    public String getEndpointAttr() {
        return endpointAttr;
    }

    public void setEndpointAttr(String endpointAttr) {
        this.endpointAttr = endpointAttr;
    }

    // getters and setters
}
