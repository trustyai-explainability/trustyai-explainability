package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import org.kie.trustyai.service.payloads.consumer.partial.PartialPayload;
import org.kie.trustyai.service.payloads.consumer.partial.PartialPayloadId;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

public class HibernatePayloadStorage<T extends PartialPayload, U extends PartialPayload> extends PayloadStorage<T, U> {
    @Inject
    protected EntityManager em;

    @Override
    @Transactional
    public void addUnreconciledInput(String id, PartialPayload inputPayload) {
        if (hasUnreconciledInput(id)) {
            em.merge(inputPayload);
        } else {
            em.persist(inputPayload);
        }
    }

    @Override
    @Transactional
    public void addUnreconciledOutput(String id, PartialPayload outputPayload) {
        if (hasUnreconciledOutput(id)) {
            em.merge(outputPayload);
        } else {
            em.persist(outputPayload);
        }
    }

    @Override
    @Transactional
    public boolean hasUnreconciledInput(String id) {
        return em.find(PartialPayload.class, PartialPayloadId.request(id)) != null;
    }

    @Override
    @Transactional
    public boolean hasUnreconciledOutput(String id) {
        return em.find(PartialPayload.class, PartialPayloadId.response(id)) != null;
    }

    @Override
    @Transactional
    public T getUnreconciledInput(String id) {
        return (T) em.find(PartialPayload.class, PartialPayloadId.request(id));
    }

    @Override
    @Transactional
    public U getUnreconciledOutput(String id) {
        return (U) em.find(PartialPayload.class, PartialPayloadId.response(id));
    }

    @Override
    @Transactional
    public void removeUnreconciledInput(String id) {
        em.remove(em.find(PartialPayload.class, PartialPayloadId.request(id)));
    }

    @Override
    @Transactional
    public void removeUnreconciledOutput(String id) {
        em.remove(em.find(PartialPayload.class, PartialPayloadId.response(id)));
    }

    @Override
    @Transactional
    public void clear() {
        em.createQuery("delete from PartialPayload").executeUpdate();
    }
}
