package org.kie.trustyai.service.payloads.consumer.partial;

import java.util.Map;

public interface PartialPayloadInterface {

    String getId();

    void setId(String id);

    String getModelId();

    void setModelId(String modelId);

    Map<String, String> getMetadata();

    PartialKind getKind();
}
