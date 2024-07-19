package org.kie.trustyai.service.data.reconcilers;

import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.reconcilers.payloadstorage.PayloadStorage;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.PartialPayload;

public abstract class InferencePayloadReconciler<T extends PartialPayload, U extends PartialPayload> {

    @Inject
    Instance<PayloadStorage<T, U>> payloadStorage;

    /**
     * Add a {@link InferencePartialPayload} input to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) output {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     *
     * @param input
     */
    public synchronized void addUnreconciledInput(T input) throws InvalidSchemaException, DataframeCreateException {
        final String id = input.getId();

        payloadStorage.get().addUnreconciledInput(id, input);
        if (payloadStorage.get().hasUnreconciledOutput(id)) {
            save(id, input.getModelId());
        }
    }

    /**
     * Add a {@link InferencePartialPayload} output to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) input {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     *
     * @param output
     */
    public synchronized void addUnreconciledOutput(U output) throws InvalidSchemaException, DataframeCreateException {
        final String id = output.getId();
        payloadStorage.get().addUnreconciledOutput(id, output);
        if (payloadStorage.get().hasUnreconciledInput(id)) {
            save(id, output.getModelId());
        }
    }

    abstract protected void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException;

    abstract public Dataframe payloadToDataframe(T inputPayload, U outputPayload, String id, Map<String, String> metadata) throws DataframeCreateException;

    public void clear() {
        payloadStorage.get().clear();
    }
}
