package org.kie.trustyai.service.payloads.data.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelInferJointPayload {
    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("data_tag")
    private String dataTag;

    @JsonProperty("is_ground_truth")
    private boolean groundTruth;

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

    public boolean isGroundTruth() {
        return groundTruth;
    }

    public void setGroundTruth(boolean groundTruth) {
        this.groundTruth = groundTruth;
    }

    @Override
    public String toString() {
        return "ModelInferJointPayload{" +
                "modelName='" + modelName + '\'' +
                ", dataTag='" + dataTag + '\'' +
                ", groundTruth=" + groundTruth +
                ", request=" + request +
                ", response=" + response +
                '}';
    }
}
