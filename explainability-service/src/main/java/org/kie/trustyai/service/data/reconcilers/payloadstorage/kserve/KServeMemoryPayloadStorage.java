package org.kie.trustyai.service.data.reconcilers.payloadstorage.kserve;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.MemoryPayloadStorage;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;

import io.quarkus.arc.lookup.LookupUnlessProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupUnlessProperty(name = "service.storage-format", stringValue = "DATABASE")
@ApplicationScoped
public class KServeMemoryPayloadStorage extends MemoryPayloadStorage<KServeInputPayload, KServeOutputPayload> implements KServePayloadStorage {
}
