package org.kie.trustyai.service.mocks.kserve;

import io.quarkus.funqy.knative.events.CloudEvent;

public class MockKServeInputPayload extends MockKServePayload<byte[]> {

    public static CloudEvent<byte[]> create(String id, byte[] data, String modelId) {
        final MockKServeInputPayload payload = new MockKServeInputPayload();
        payload.setId(id);
        payload.setData(data);
        payload.extensions().put("Inferenceservicename", modelId);
        return payload;
    }

}
