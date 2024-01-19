package org.kie.trustyai.service.payloads.data.download;

public class ModelDataRequestPayload extends DataRequestPayload {
    String modelId;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public String toString() {
        return "ModelDataRequestPayload{" +
                "modelId='" + modelId + '\'' +
                ", matchAny=" + matchAny +
                ", matchAll=" + matchAll +
                ", matchNone=" + matchNone +
                '}';
    }
}
