package org.kie.trustyai.service.data.utils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.PayloadParser;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@Singleton
public class InferencePayloadReconciler {
    private static final Logger LOG = Logger.getLogger(InferencePayloadReconciler.class);
    private final Map<String, InferencePartialPayload> unreconciledInputs = new ConcurrentHashMap<>();
    private final Map<String, InferencePartialPayload> unreconciledOutputs = new ConcurrentHashMap<>();

    @Inject
    Instance<DataSource> datasource;

    /**
     * Add a {@link InferencePartialPayload} input to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) output {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     * 
     * @param input
     */
    public synchronized void addUnreconciledInput(InferencePartialPayload input) throws InvalidSchemaException, DataframeCreateException {
        final String id = input.getId();
        unreconciledInputs.put(id, input);
        if (unreconciledOutputs.containsKey(id)) {
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
    public synchronized void addUnreconciledOutput(InferencePartialPayload output) throws InvalidSchemaException, DataframeCreateException {
        final String id = output.getId();
        unreconciledOutputs.put(id, output);
        if (unreconciledInputs.containsKey(id)) {
            save(id, output.getModelId());
        }
    }

    private synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final InferencePartialPayload output = unreconciledOutputs.get(id);
        final InferencePartialPayload input = unreconciledInputs.get(id);
        LOG.debug("Reconciling partial input and output, id=" + id);

        // save
        final byte[] inputBytes = Base64.getDecoder().decode(input.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(output.getData().getBytes());

        final Prediction prediction = payloadToPrediction(inputBytes, outputBytes);
        final Dataframe dataframe = Dataframe.createFrom(prediction);

        datasource.get().saveDataframe(dataframe, modelId);

        unreconciledInputs.remove(id);
        unreconciledOutputs.remove(id);

    }

    /**
     * Convert both input and output {@link InferencePartialPayload} to a TrustyAI {@link Prediction}.
     * 
     * @param inputs KServe v2 protobuf raw bytes
     * @param outputs KServe v2 protobuf raw bytes
     * @return A {@link Prediction}
     * @throws DataframeCreateException
     */
    public Prediction payloadToPrediction(byte[] inputs, byte[] outputs) throws DataframeCreateException {
        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final PredictionInput predictionInput;
        try {
            predictionInput = PayloadParser
                    .inputTensorToPredictionInput(input.getInputs(0), null);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInput.getFeatures());

        // Check for dataframe metadata name conflicts
        if (predictionInput.getFeatures()
                .stream()
                .map(Feature::getName)
                .anyMatch(name -> name.equals(MetadataUtils.ID_FIELD) || name.equals(MetadataUtils.TIMESTAMP_FIELD))) {
            final String message = "An input feature as a protected name: \"_id\" or \"_timestamp\"";
            LOG.error(message);
            throw new DataframeCreateException(message);
        }

        // enrich with data and id
        final List<Feature> features = new ArrayList<>();
        features.add(FeatureFactory.newObjectFeature(MetadataUtils.ID_FIELD, UUID.randomUUID()));
        features.add(FeatureFactory.newObjectFeature(MetadataUtils.TIMESTAMP_FIELD, LocalDateTime.now()));
        features.addAll(predictionInput.getFeatures());

        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final PredictionOutput predictionOutput;
        try {
            predictionOutput = PayloadParser
                    .outputTensorToPredictionOutput(output.getOutputs(0), null);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }
        LOG.debug("Prediction output: " + predictionOutput.getOutputs());

        return new SimplePrediction(new PredictionInput(features), predictionOutput);
    }

}
