package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import org.kie.trustyai.service.payloads.consumer.PartialPayload;

public interface PayloadStorageInterface<T extends PartialPayload, U extends PartialPayload> {
    void addUnreconciledInput(String id, T inputPayload);

    void addUnreconciledOutput(String id, U outputPayload);

    boolean hasUnreconciledInput(String id);

    boolean hasUnreconciledOutput(String id);

    T getUnreconciledInput(String id);

    U getUnreconciledOutput(String id);

    void removeUnreconciledInput(String id);

    void removeUnreconciledOutput(String id);

    void clear();
}
