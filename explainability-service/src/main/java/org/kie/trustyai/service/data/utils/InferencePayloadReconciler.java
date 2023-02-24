package org.kie.trustyai.service.data.utils;

import java.time.LocalDateTime;
import java.util.*;

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
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;

import com.google.protobuf.InvalidProtocolBufferException;

@Singleton
public class InferencePayloadReconciler {
    private static final Logger LOG = Logger.getLogger(InferencePayloadReconciler.class);
    private final Map<UUID, InferencePartialPayload> unreconciledInputs = new HashMap<>();
    private final Map<UUID, InferencePartialPayload> unreconciledOutputs = new HashMap<>();

    @Inject
    Instance<DataSource> datasource;

    public void addUnreconciledInput(InferencePartialPayload input) {
        final UUID id = input.getId();
        unreconciledInputs.put(id, input);
        if (unreconciledOutputs.containsKey(id)) {
            save(id, input.getModelId());
        }
    }

    public void addUnreconciledOutput(InferencePartialPayload output) {
        final UUID id = output.getId();
        unreconciledOutputs.put(id, output);
        if (unreconciledInputs.containsKey(id)) {
            save(id, output.getModelId());
        }
    }

    private void save(UUID id, String modelId) {
        final InferencePartialPayload output = unreconciledOutputs.get(id);
        final InferencePartialPayload input = unreconciledInputs.get(id);
        LOG.info("Saving: " + output + " and " + input);

        // save
        final byte[] inputBytes = Base64.getDecoder().decode(input.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(output.getData().getBytes());

        final Prediction prediction = save(inputBytes, outputBytes);
        final Dataframe dataframe = Dataframe.createFrom(prediction);

        datasource.get().saveDataframe(dataframe, modelId);

        unreconciledInputs.remove(id);
        unreconciledOutputs.remove(id);

    }

    public Prediction save(byte[] inputs, byte[] outputs) throws DataframeCreateException {
        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final PredictionInput predictionInput = PayloadParser
                .inputTensorToPredictionInput(input.getInputs(0), null);
        LOG.info(predictionInput.getFeatures());

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
        final PredictionOutput predictionOutput = PayloadParser
                .outputTensorToPredictionOutput(output.getOutputs(0), null);
        LOG.info(predictionOutput.getOutputs());

        return new SimplePrediction(new PredictionInput(features), predictionOutput);

    }

}
