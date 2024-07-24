package org.kie.trustyai.service.data.reconcilers.payloadstorage.kserve;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.PayloadStorageInterface;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;

public interface KServePayloadStorage extends PayloadStorageInterface<KServeInputPayload, KServeOutputPayload> {
}
