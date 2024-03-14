package org.kie.trustyai.service.data.utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;

import com.google.protobuf.InvalidProtocolBufferException;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@Singleton
public class ModelMeshInferencePayloadReconciler extends InferencePayloadReconciler<InferencePartialPayload, InferencePartialPayload> {
    private static final Logger LOG = Logger.getLogger(ModelMeshInferencePayloadReconciler.class);

    @Inject
    Instance<DataSource> datasource;

    protected static final String MM_MODEL_SUFFIX = "__isvc";

    protected static String standardizeModelId(String inboundModelId) {
        if (inboundModelId != null && inboundModelId.contains(MM_MODEL_SUFFIX)) {
            int index = inboundModelId.lastIndexOf(MM_MODEL_SUFFIX);
            return inboundModelId.substring(0, index);
        } else {
            return inboundModelId;
        }
    }

    protected synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final InferencePartialPayload output = unreconciledOutputs.get(id);
        final InferencePartialPayload input = unreconciledInputs.get(id);
        LOG.debug("Reconciling partial input and output, id=" + id);

        // save
        final Dataframe dataframe = payloadToDataframe(input, output, id, input.getMetadata());

        datasource.get().saveDataframe(dataframe, standardizeModelId(modelId));

        unreconciledInputs.remove(id);
        unreconciledOutputs.remove(id);
    }

    /**
     * Convert both input and output {@link InferencePartialPayload} to a TrustyAI {@link Prediction}.
     * 
     * @param inputPayload Input {@link InferencePartialPayload}
     * @param outputPayload Output {@link InferencePartialPayload}
     * @param id The unique id of the payload
     * @return A {@link Prediction}
     * @throws DataframeCreateException
     */
    public Dataframe payloadToDataframe(InferencePartialPayload inputPayload, InferencePartialPayload outputPayload, String id, Map<String, String> metadata) throws DataframeCreateException {
        final byte[] inputBytes = Base64.getDecoder().decode(inputPayload.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(outputPayload.getData().getBytes());

        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionInput> predictionInput;
        final int enforcedFirstDimension;
        try {
            predictionInput = TensorConverter.parseKserveModelInferRequest(input);
            enforcedFirstDimension = predictionInput.size();
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInput);
        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionOutput> predictionOutput;
        try {
            predictionOutput = TensorConverter.parseKserveModelInferResponse(output, enforcedFirstDimension);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }
        LOG.debug("Prediction output: " + predictionOutput);

        // Aggregate features and outputs
        final int size = predictionInput.size();
        final List<Prediction> predictions = IntStream.range(0, size).mapToObj(i -> {
            final PredictionInput pi = predictionInput.get(i);
            final PredictionOutput po = predictionOutput.get(i);
            return new SimplePrediction(pi, po);
        }).collect(Collectors.toCollection(ArrayList::new));

        final Dataframe dataframe = Dataframe.createFrom(predictions);

        dataframe.setInputTensorName(input.getInputs(0).getName());
        dataframe.setOutputTensorName(output.getOutputs(0).getName());

        return dataframe;
    }
}
