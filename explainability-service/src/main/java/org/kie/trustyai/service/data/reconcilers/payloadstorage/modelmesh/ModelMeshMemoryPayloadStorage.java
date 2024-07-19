package org.kie.trustyai.service.data.reconcilers.payloadstorage.modelmesh;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.MemoryPayloadStorage;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;

import io.quarkus.arc.lookup.LookupUnlessProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@LookupUnlessProperty(name = "service.storage-format", stringValue = "DATABASE")
public class ModelMeshMemoryPayloadStorage extends MemoryPayloadStorage<InferencePartialPayload, InferencePartialPayload> implements ModelMeshPayloadStorage {
}
