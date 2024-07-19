package org.kie.trustyai.service.data.reconcilers.payloadstorage.kserve;

import org.kie.trustyai.service.data.reconcilers.payloadstorage.HibernatePayloadStorage;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupIfProperty(name = "service.storage-format", stringValue = "DATABASE")
@ApplicationScoped
public class KServeHibernatePayloadStorage extends HibernatePayloadStorage<KServeInputPayload, KServeOutputPayload> implements KServePayloadStorage {
}
