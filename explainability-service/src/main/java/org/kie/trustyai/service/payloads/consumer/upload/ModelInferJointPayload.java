package org.kie.trustyai.service.payloads.consumer.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelInferJointPayload {
    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("data_tag")
    private String dataTag;

    private ModelInferRequestPayload request;
    private ModelInferResponsePayload response;

    public String getModelName() {
        return modelName;
    }

    public ModelInferRequestPayload getRequest() {
        return request;
    }

    public void setRequest(ModelInferRequestPayload request) {
        this.request = request;
    }

    public ModelInferResponsePayload getResponse() {
        return response;
    }

    public void setResponse(ModelInferResponsePayload response) {
        this.response = response;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDataTag() {
        return dataTag;
    }

    public void setDataTag(String dataTag) {
        this.dataTag = dataTag;
    }

    @Override
    public String toString() {
        return "ModelJointPayload{" +
                "modelName='" + modelName + '\'' +
                ", request=" + request +
                ", response=" + response +
                '}';
    }
}
