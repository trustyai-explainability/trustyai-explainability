package org.kie.trustyai.service.data.reconcilers.payloadstorage;

import io.quarkus.arc.lookup.LookupUnlessProperty;
import jakarta.ejb.ApplicationException;
import org.kie.trustyai.service.payloads.consumer.PartialPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@LookupUnlessProperty(name = "service.storage.format", stringValue = "DATABASE")
@ApplicationException
public class MemoryPayloadStorage<T extends PartialPayload, U extends PartialPayload> extends PayloadStorage<T , U > {

    protected final Map<String, T> unreconciledInputs = new ConcurrentHashMap<>();
    protected final Map<String, U> unreconciledOutputs = new ConcurrentHashMap<>();

    @Override
    public void addUnreconciledInput(String id, T inputPayload) {
        unreconciledInputs.put(id, inputPayload);
    }

    @Override
    public void addUnreconciledOutput(String id, U outputPayload) {
        unreconciledOutputs.put(id, outputPayload);
    }

    @Override
    public boolean hasUnreconciledInput(String id) {
        return unreconciledInputs.containsKey(id);
    }

    @Override
    public boolean hasUnreconciledOutput(String id) {
        return unreconciledOutputs.containsKey(id);
    }

    @Override
    public T getUnreconciledInput(String id) {
        return unreconciledInputs.get(id);
    }

    @Override
    public U getUnreconciledOutput(String id) {
        return unreconciledOutputs.get(id);
    }

    @Override
    public void removeUnreconciledInput(String id) {
        unreconciledInputs.remove(id);
    }

    @Override
    public  void removeUnreconciledOutput(String id) {
        unreconciledOutputs.remove(id);

    }

    @Override
    public void clear() {
        unreconciledInputs.clear();
        unreconciledOutputs.clear();
    }


}
