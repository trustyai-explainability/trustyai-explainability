package org.kie.trustyai.service.payloads.consumer;

import java.util.Map;

public interface PartialPayload {

    String getId();

    void setId(String id);

    String getModelId();

    void setModelId(String modelId);

    Map<String, String> getMetadata();

}
