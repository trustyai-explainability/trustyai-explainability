package org.kie.trustyai.service.data.reconcilers;

import java.util.Map;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.reconcilers.payloadstorage.PayloadStorageInterface;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.partial.PartialPayload;

public abstract class InferencePayloadReconciler<T extends PartialPayload, U extends PartialPayload, V extends PayloadStorageInterface<T, U>> {
    /**
     * Add a {@link InferencePartialPayload} input to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) output {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     *
     * @param input
     */
    public synchronized void addUnreconciledInput(T input) throws InvalidSchemaException, DataframeCreateException {
        final String id = input.getId();

        getPayloadStorage().addUnreconciledInput(id, input);
        if (getPayloadStorage().hasUnreconciledOutput(id)) {
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
        getPayloadStorage().addUnreconciledOutput(id, output);
        if (getPayloadStorage().hasUnreconciledInput(id)) {
            save(id, output.getModelId());
        }
    }

    abstract V getPayloadStorage();

    abstract protected void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException;

    abstract public Dataframe payloadToDataframe(T inputPayload, U outputPayload, String id, Map<String, String> metadata) throws DataframeCreateException;

    public void clear() {
        getPayloadStorage().clear();
    }

}
