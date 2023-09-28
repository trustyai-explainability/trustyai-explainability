package org.kie.trustyai.service.mocks;

import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;

import io.quarkus.funqy.knative.events.CloudEvent;

public class MockKServeOutputPayload extends MockKServePayload<InferenceLoggerOutput> {
    public static CloudEvent<InferenceLoggerOutput> create(String id, InferenceLoggerOutput data, String modelId) {
        final MockKServeOutputPayload payload = new MockKServeOutputPayload();
        payload.setId(id);
        payload.setData(data);
        payload.extensions().put("Inferenceservicename", modelId);
        return payload;
    }
}
