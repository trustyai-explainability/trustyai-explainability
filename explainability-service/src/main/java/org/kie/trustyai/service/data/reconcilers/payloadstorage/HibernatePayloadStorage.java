package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.kie.trustyai.service.payloads.consumer.PartialPayload;

@LookupIfProperty(name = "service.storage.format", stringValue = "DATABASE")
public class HibernatePayloadStorage<T extends PartialPayload, U extends PartialPayload> extends PayloadStorage<T, U> {
    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void addUnreconciledInput(String id, PartialPayload inputPayload) {
        em.persist(inputPayload);
    }

    @Override
    @Transactional
    public void addUnreconciledOutput(String id, PartialPayload outputPayload) {
        em.persist(outputPayload);
    }

    @Override
    @Transactional
    public boolean hasUnreconciledInput(String id) {
        return em.find(T, id) != null;
    }

    @Override
    @Transactional
    public boolean hasUnreconciledOutput(String id) {
        return false;
    }

    @Override
    @Transactional
    public PartialPayload getUnreconciledInput(String id) {
        return null;
    }

    @Override
    @Transactional
    public  PartialPayload getUnreconciledOutput(String id) {
        return null;
    }

    @Override
    @Transactional
    public void removeUnreconciledInput(String id) {

    }

    @Override
    @Transactional
    public void removeUnreconciledOutput(String id) {

    }

    @Override
    @Transactional
    public void clear() {

    }
}
