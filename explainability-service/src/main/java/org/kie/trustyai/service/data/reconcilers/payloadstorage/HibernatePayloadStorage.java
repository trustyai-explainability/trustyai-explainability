package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import org.hibernate.exception.DataException;
import org.kie.trustyai.service.data.exceptions.PayloadWriteException;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;
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
    public void addUnreconciledInput(String id, PartialPayload inputPayload) throws PayloadWriteException {
        try {
            if (hasUnreconciledInput(id)) {
                em.merge(inputPayload);
            } else {
                em.persist(inputPayload);
            }
            // force the transaction to commit, and therefore throw any errors *inside* the try/catch block
            em.flush();
        } catch (DataException e) {
            if (inputPayload instanceof InferencePartialPayload) {
                InferencePartialPayload payload = (InferencePartialPayload) inputPayload;
                int payloadSize = payload.getData().getBytes().length;
                throw new PayloadWriteException(String.format(
                        "Could not persist partial input payload of size=%,d bytes. This can happen if the payload is too large, try reducing inference batch size.", payloadSize));
            } else {
                throw new PayloadWriteException("Could not persist partial input payload. This can happen if the payload is too large, try reducing inference batch size.");
            }

        }
    }

    @Override
    @Transactional
    public void addUnreconciledOutput(String id, PartialPayload outputPayload) throws PayloadWriteException {
        try {
            if (hasUnreconciledOutput(id)) {
                em.merge(outputPayload);
            } else {
                em.persist(outputPayload);
            }
            // force the transaction to commit, and therefore throw any errors *inside* the try/catch block
            em.flush();
        } catch (DataException e) {
            if (outputPayload instanceof InferencePartialPayload) {
                InferencePartialPayload payload = (InferencePartialPayload) outputPayload;
                int payloadSize = payload.getData().length();
                throw new PayloadWriteException(String.format(
                        "Could not persist partial output payload of size=%,d bytes. This can happen if the payload is too large, try reducing inference batch size.", payloadSize));
            } else {
                throw new PayloadWriteException("Could not persist partial output payload. This can happen if the payload is too large, try reducing inference batch size.");
            }
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
