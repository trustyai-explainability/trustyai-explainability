package org.kie.trustyai.service.data.reconcilers.payloadstorage.modelmesh;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.HibernatePayloadStorage;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupIfProperty(name = "service.storage-format", stringValue = "DATABASE")
@ApplicationScoped
public class ModelMeshHibernatePayloadStorage extends HibernatePayloadStorage<InferencePartialPayload, InferencePartialPayload> implements ModelMeshPayloadStorage {
}
