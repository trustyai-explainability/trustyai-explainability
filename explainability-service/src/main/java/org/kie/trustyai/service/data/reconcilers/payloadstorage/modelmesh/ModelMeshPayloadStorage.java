package org.kie.trustyai.service.data.reconcilers.payloadstorage.modelmesh;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.PayloadStorageInterface;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;

public interface ModelMeshPayloadStorage extends PayloadStorageInterface<InferencePartialPayload, InferencePartialPayload> {
}
